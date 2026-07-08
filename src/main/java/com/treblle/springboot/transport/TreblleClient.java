package com.treblle.springboot.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.PayloadFactory;
import com.treblle.springboot.core.TreblleLogger;
import com.treblle.springboot.core.model.TrebllePayload;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Fire-and-forget transport to Treblle Ingress.
 *
 * <ul>
 *   <li>Builds and masks the payload on a worker thread — never on the request thread.</li>
 *   <li>GZIP-compresses the payload.</li>
 *   <li>Sends with a shared, connection-pooling {@link HttpClient} (keep-alive).</li>
 *   <li>Uses a bounded queue + a small worker pool so a Treblle outage can never grow host memory;
 *       when the queue is full the oldest task is dropped.</li>
 *   <li>Applies a short hard timeout and never retries in the request path.</li>
 *   <li>Consults the {@link CircuitBreaker} before every send and feeds it the response status.</li>
 * </ul>
 *
 * <p>The request thread only hands over a {@link RawRequestData} (cheap references it already
 * captured). All expensive work — payload building, masking, serialization, compression, HTTP —
 * happens here, off-thread.</p>
 *
 * <p>Every operation is wrapped so it can never throw into the host application.</p>
 */
public class TreblleClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(2_500);
    private static final int QUEUE_CAPACITY = 1_000;
    private static final int WORKER_THREADS = 2;

    private final TreblleProperties properties;
    private final ObjectMapper objectMapper;
    private final TreblleLogger logger;
    private final CircuitBreaker circuitBreaker;
    private final PayloadFactory payloadFactory;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final BlockingQueue<Runnable> queue;

    public TreblleClient(TreblleProperties properties,
                         ObjectMapper objectMapper,
                         TreblleLogger logger,
                         CircuitBreaker circuitBreaker,
                         PayloadFactory payloadFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.logger = logger;
        this.circuitBreaker = circuitBreaker;
        this.payloadFactory = payloadFactory;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(2_000))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.executor = new java.util.concurrent.ThreadPoolExecutor(
                WORKER_THREADS, WORKER_THREADS, 0L, TimeUnit.MILLISECONDS,
                queue, daemonThreadFactory(),
                // Drop-oldest: make room by discarding the head of the queue.
                new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread t = new Thread(runnable, "treblle-dispatcher-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        };
    }

    /**
     * Enqueues captured request data for asynchronous processing and delivery. Returns immediately;
     * the host request thread is never blocked. The payload is built, masked, serialized, compressed
     * and sent entirely on a worker thread. Dropped silently (with a debug log) if the queue is
     * saturated, or later if the circuit breaker is backing off.
     */
    public void send(RawRequestData raw) {
        try {
            executor.execute(() -> process(raw));
        } catch (Throwable t) {
            // Queue saturated or executor rejected: drop silently.
            logger.warn("Payload dropped: dispatcher unavailable (" + t.getClass().getSimpleName() + ").");
        }
    }

    private void process(RawRequestData raw) {
        try {
            if (!circuitBreaker.tryAcquire()) {
                logger.info("Payload dropped: circuit breaker is backing off.");
                return;
            }
            dispatch(payloadFactory.build(raw));
        } catch (Throwable t) {
            logger.error("Failed to build Treblle payload.", t);
        }
    }

    private void dispatch(TrebllePayload payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            byte[] gzipped = gzip(json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getIngressEndpoint()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Content-Encoding", "gzip")
                    .header("x-api-key", properties.getSdkToken())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(gzipped))
                    .build();

            logger.info("Sending payload to " + properties.getIngressEndpoint()
                    + " (" + gzipped.length + " bytes gzipped).");

            CompletableFuture<HttpResponse<String>> future =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            future.whenComplete((response, throwable) -> handleResponse(response, throwable));
        } catch (Throwable t) {
            logger.error("Failed to build/send Treblle payload.", t);
            circuitBreaker.onFailure(null);
        }
    }

    private void handleResponse(HttpResponse<String> response, Throwable throwable) {
        try {
            if (throwable != null) {
                logger.error("Network error sending payload to Treblle.", throwable);
                circuitBreaker.onFailure(null);
                return;
            }
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                logger.info("Treblle responded " + status + ".");
                circuitBreaker.onSuccess();
            } else if (status == 429) {
                Long retryAfter = CircuitBreaker.parseRetryAfter(
                        response.headers().firstValue("Retry-After").orElse(null));
                logger.warn("Treblle responded 429; Retry-After=" + retryAfter + "s.");
                circuitBreaker.onFailure(retryAfter);
            } else {
                logger.warn("Treblle responded " + status + "; body: " + truncate(response.body()));
                circuitBreaker.onFailure(null);
            }
        } catch (Throwable t) {
            logger.error("Error handling Treblle response.", t);
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    static byte[] gzip(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(64, data.length / 2));
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    /** Graceful shutdown of the dispatcher pool. */
    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (Throwable ignored) {
            executor.shutdownNow();
        }
    }

    /** For tests: turn a UTF-8 JSON string into a compressed body. */
    static byte[] gzipUtf8(String s) throws Exception {
        return gzip(s.getBytes(StandardCharsets.UTF_8));
    }
}
