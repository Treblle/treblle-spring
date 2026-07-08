package com.treblle.springboot.filter;

import com.treblle.springboot.collector.ErrorCollector;
import com.treblle.springboot.collector.RawRequestData;
import com.treblle.springboot.config.TreblleProperties;
import com.treblle.springboot.core.PathMatcher;
import com.treblle.springboot.core.TreblleLogger;
import com.treblle.springboot.core.UrlEncodedParser;
import com.treblle.springboot.metadata.TreblleMetadata;
import com.treblle.springboot.transport.TreblleClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Collector. A {@link OncePerRequestFilter} that wraps each request/response, lets the chain
 * run, flushes the response body to the client first, then normalizes → masks → sends off-thread.
 *
 * <p>Hot-path discipline: during the request we only capture cheap references into a
 * {@link RawRequestData}. The response body is flushed back to the client <em>first</em>, then the
 * captured data is handed to the transport, which does all expensive work (payload building,
 * masking, serialization, compression, HTTP) on its own worker threads. The request thread is never
 * blocked by Treblle.</p>
 *
 * <p>The filter never swallows or alters host errors. Exceptions from the chain are recorded then
 * rethrown unchanged so the framework's own error handling behaves exactly as normal.</p>
 */
public class TreblleFilter extends OncePerRequestFilter {

    private final TreblleProperties properties;
    private final TreblleClient client;
    private final PathMatcher pathMatcher;
    private final TreblleLogger logger;
    private final boolean active;

    public TreblleFilter(TreblleProperties properties,
                         TreblleClient client,
                         PathMatcher pathMatcher,
                         TreblleLogger logger,
                         boolean active) {
        this.properties = properties;
        this.client = client;
        this.pathMatcher = pathMatcher;
        this.logger = logger;
        this.active = active;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // When disabled or missing credentials, act as a pass-through with zero overhead.
        if (!active || !properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!pathMatcher.shouldTrack(path)) {
            logger.info("Skipping untracked path: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        Throwable captured = null;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Throwable t) {
            // Observe, never interfere: record then rethrow unchanged.
            ErrorCollector.record("onException", t);
            captured = t;
            throw t;
        } finally {
            double loadTimeMs = (System.nanoTime() - start) / 1_000_000.0;
            RawRequestData raw = null;
            try {
                // When an exception propagated out of the chain, the servlet container turns it into
                // a 5xx during a later ERROR dispatch that this filter does not observe. Reflect that
                // in the captured status so the payload matches what the client ultimately receives.
                int effectiveStatus = wrappedResponse.getStatus();
                if (captured != null && effectiveStatus < 500) {
                    effectiveStatus = 500;
                }
                // If a 5xx slipped through without an exception, record it as onError.
                if (captured == null && effectiveStatus >= 500) {
                    ErrorCollector.record("onError",
                            new RuntimeException("Server responded with status " + effectiveStatus));
                }
                // Cheap capture only — snapshots the request-thread data (bodies, headers, errors,
                // metadata) into a detached holder. No masking, parsing, or serialization here.
                raw = capture(wrappedRequest, wrappedResponse, loadTimeMs, effectiveStatus);
            } catch (Throwable sdkError) {
                // The SDK must never break the host application.
                logger.error("Treblle capture failed; request unaffected.", sdkError);
            } finally {
                // CRITICAL: flush cached response body back to the real client, before anything else.
                try {
                    wrappedResponse.copyBodyToResponse();
                } catch (Throwable t) {
                    logger.error("Failed to copy response body.", t);
                }
                ErrorCollector.clear();
                TreblleMetadata.clear();
            }
            // Response is already on its way to the client; hand off for off-thread processing.
            if (raw != null) {
                try {
                    client.send(raw);
                } catch (Throwable sdkError) {
                    logger.error("Treblle hand-off failed; request unaffected.", sdkError);
                }
            }
        }
    }

    private RawRequestData capture(ContentCachingRequestWrapper request,
                                   ContentCachingResponseWrapper response,
                                   double loadTimeMs,
                                   int effectiveStatus) {
        RawRequestData raw = new RawRequestData();

        raw.setMethod(request.getMethod());
        raw.setUrl(buildUrl(request));
        raw.setRoutePath(resolveRoutePath(request));
        raw.setClientIp(resolveClientIp(request));
        raw.setServerIp(request.getLocalAddr());
        raw.setUserAgent(request.getHeader("User-Agent"));
        raw.setProtocol(request.getProtocol());
        raw.setServerSoftware(request.getServletContext() != null
                ? request.getServletContext().getServerInfo() : null);

        raw.setRequestHeaders(collectRequestHeaders(request));
        raw.setQueryParams(collectQueryParams(request));
        raw.setRequestContentType(request.getContentType());
        raw.setRequestContentEncoding(request.getHeader("Content-Encoding"));
        raw.setRequestBody(request.getContentAsByteArray());
        raw.setUploadedFiles(collectUploadedFiles(request));

        raw.setStatusCode(effectiveStatus);
        raw.setResponseHeaders(collectResponseHeaders(response));
        raw.setResponseContentType(response.getContentType());
        raw.setResponseContentEncoding(response.getHeader("Content-Encoding"));
        byte[] responseBody = response.getContentAsByteArray();
        raw.setResponseBody(responseBody);
        raw.setResponseSize(responseBody == null ? 0 : responseBody.length);
        raw.setLoadTimeMs(loadTimeMs);

        // Snapshot the thread-local collectors before they are cleared; the transport reads these
        // off-thread, so they must be detached copies bound to this request.
        raw.setErrors(ErrorCollector.snapshot());
        raw.setMetadata(TreblleMetadata.snapshot());
        return raw;
    }

    /**
     * Builds the request URL <em>without</em> the query string. Query parameters are captured
     * separately in the (masked) {@code query} map; keeping them out of the raw URL prevents
     * sensitive values from bypassing masking here.
     */
    private String buildUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    private String resolveRoutePath(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");
        if (pattern != null) {
            return pattern.toString();
        }
        return request.getRequestURI();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Last value wins per spec; but for client IP the first hop is the real client.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> collectRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        var names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                List<String> values = new ArrayList<>();
                var e = request.getHeaders(name);
                while (e != null && e.hasMoreElements()) {
                    values.add(e.nextElement());
                }
                if (!values.isEmpty()) {
                    headers.put(name.toLowerCase(), String.join(", ", values));
                }
            }
        }
        return headers;
    }

    private Map<String, String> collectResponseHeaders(ContentCachingResponseWrapper response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            List<String> values = new ArrayList<>(response.getHeaders(name));
            if (!values.isEmpty()) {
                // Multiple values for one header are combined per RFC 7230.
                headers.put(name.toLowerCase(), String.join(", ", values));
            }
        }
        return headers;
    }

    /**
     * Best-effort capture of uploaded file metadata (name, size, MIME) for multipart requests. The
     * file contents are never captured. Parts are already parsed by Spring's multipart resolver, so
     * this only enumerates them; any failure is swallowed so the host request is never affected.
     */
    private List<RawRequestData.FilePart> collectUploadedFiles(HttpServletRequest request) {
        List<RawRequestData.FilePart> files = new ArrayList<>();
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return files;
        }
        try {
            for (Part part : request.getParts()) {
                String fileName = part.getSubmittedFileName();
                if (fileName != null) {
                    files.add(new RawRequestData.FilePart(fileName, part.getSize(), part.getContentType()));
                }
            }
        } catch (Throwable t) {
            logger.warn("Could not read multipart parts: " + t.getClass().getSimpleName() + ".");
        }
        return files;
    }

    /**
     * Parses the raw query string so the {@code query} map contains only URL query parameters (not
     * form-body parameters, which {@code getParameterMap()} would merge in). Repeated keys: last
     * value wins.
     */
    private Map<String, String> collectQueryParams(HttpServletRequest request) {
        return UrlEncodedParser.parse(request.getQueryString());
    }
}
