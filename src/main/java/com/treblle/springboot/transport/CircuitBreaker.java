package com.treblle.springboot.transport;

import com.treblle.springboot.core.TreblleLogger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free three-state circuit breaker that stops the SDK from spamming Ingress while it is
 * rejecting requests. Uses an {@link AtomicReference} to a {@link ThrottleState} so the hot-path
 * {@link #tryAcquire()} check is O(1) and never blocks the host application.
 *
 * <p>Backoff rules:</p>
 * <ul>
 *   <li>Any 4xx/5xx counts as a failure.</li>
 *   <li>On 429 with a parseable {@code Retry-After} delta-seconds header, backoff =
 *       {@code min(retryAfter, 60s)}.</li>
 *   <li>Otherwise exponential backoff starting at 1s, doubling per consecutive failure, capped at
 *       60s, with ±20% jitter.</li>
 *   <li>Two consecutive successful probes reset to Normal.</li>
 * </ul>
 *
 * <p>Fail-open: while backing off, payloads are dropped — never queued, delayed, or blocked.</p>
 */
public class CircuitBreaker {

    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 60_000L;
    private static final int PROBES_TO_RECOVER = 2;

    private final AtomicReference<ThrottleState> ref = new AtomicReference<>(ThrottleState.normal());
    private final TreblleLogger logger;

    public CircuitBreaker(TreblleLogger logger) {
        this.logger = logger;
    }

    /**
     * Hot-path gate. Returns {@code true} if a payload may be sent. In BACKING_OFF, returns
     * {@code false} unless the window has expired, in which case it transitions to PROBING and lets
     * exactly one request through.
     */
    public boolean tryAcquire() {
        while (true) {
            ThrottleState current = ref.get();
            switch (current.state()) {
                case NORMAL:
                    return true;
                case PROBING:
                    // A probe is already in flight; drop others until it resolves.
                    return false;
                case BACKING_OFF:
                    long now = System.currentTimeMillis();
                    if (now < current.retryAtEpochMs()) {
                        return false;
                    }
                    ThrottleState probing = new ThrottleState(
                            ThrottleState.State.PROBING,
                            current.backoffMillis(),
                            current.retryAtEpochMs(),
                            current.consecutiveFails(),
                            current.probeSuccesses());
                    if (ref.compareAndSet(current, probing)) {
                        logger.info("Circuit breaker entering PROBING state.");
                        return true;
                    }
                    // Lost the race; loop and re-evaluate.
                    break;
                default:
                    return true;
            }
        }
    }

    /** Records a successful send (HTTP 2xx from Ingress). */
    public void onSuccess() {
        while (true) {
            ThrottleState current = ref.get();
            if (current.state() == ThrottleState.State.NORMAL) {
                return;
            }
            if (current.state() == ThrottleState.State.PROBING) {
                int successes = current.probeSuccesses() + 1;
                if (successes >= PROBES_TO_RECOVER) {
                    if (ref.compareAndSet(current, ThrottleState.normal())) {
                        logger.info("Circuit breaker recovered; returning to NORMAL.");
                        return;
                    }
                } else {
                    // One good probe, need one more; go back to a short backoff to gate the next probe.
                    ThrottleState next = new ThrottleState(
                            ThrottleState.State.BACKING_OFF,
                            INITIAL_BACKOFF_MS,
                            System.currentTimeMillis() + INITIAL_BACKOFF_MS,
                            0,
                            successes);
                    if (ref.compareAndSet(current, next)) {
                        logger.info("Circuit breaker probe succeeded (" + successes + "/" + PROBES_TO_RECOVER + ").");
                        return;
                    }
                }
            } else {
                // A stray success while backing off; leave state as-is.
                return;
            }
        }
    }

    /**
     * Records a failure (4xx/5xx or network error).
     *
     * @param retryAfterSeconds parsed {@code Retry-After} delta-seconds on a 429, or {@code null}.
     */
    public void onFailure(Long retryAfterSeconds) {
        while (true) {
            ThrottleState current = ref.get();
            int fails = current.consecutiveFails() + 1;
            long backoff;
            if (retryAfterSeconds != null) {
                backoff = Math.min(retryAfterSeconds * 1_000L, MAX_BACKOFF_MS);
            } else {
                backoff = computeExponentialBackoff(fails);
            }
            long retryAt = System.currentTimeMillis() + backoff;
            ThrottleState next = new ThrottleState(
                    ThrottleState.State.BACKING_OFF, backoff, retryAt, fails, 0);
            if (ref.compareAndSet(current, next)) {
                logger.warn("Circuit breaker BACKING_OFF for " + backoff + "ms (failures=" + fails + ").");
                return;
            }
        }
    }

    private long computeExponentialBackoff(int fails) {
        long base = INITIAL_BACKOFF_MS;
        for (int i = 1; i < fails && base < MAX_BACKOFF_MS; i++) {
            base = Math.min(base * 2, MAX_BACKOFF_MS);
        }
        base = Math.min(base, MAX_BACKOFF_MS);
        // ±20% jitter
        double jitter = 1.0 + (ThreadLocalRandom.current().nextDouble(-0.20, 0.20));
        long jittered = (long) (base * jitter);
        return Math.max(1L, Math.min(jittered, MAX_BACKOFF_MS));
    }

    /** Visible for testing. */
    public ThrottleState.State currentState() {
        return ref.get().state();
    }

    /** Visible for testing. */
    ThrottleState currentSnapshot() {
        return ref.get();
    }

    /**
     * Visible for testing: forces the current backoff window to be considered expired, without
     * sleeping, so probe/recovery transitions can be exercised deterministically.
     */
    void onFailureWindowExpiredForTest() {
        ThrottleState current = ref.get();
        if (current.state() == ThrottleState.State.BACKING_OFF) {
            ref.set(new ThrottleState(
                    ThrottleState.State.BACKING_OFF,
                    current.backoffMillis(),
                    System.currentTimeMillis() - 1,
                    current.consecutiveFails(),
                    current.probeSuccesses()));
        }
    }

    /**
     * Parses only the delta-seconds form of {@code Retry-After} (e.g. {@code "30"}). The HTTP-date
     * form falls through to {@code null}.
     */
    public static Long parseRetryAfter(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(headerValue.trim());
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
