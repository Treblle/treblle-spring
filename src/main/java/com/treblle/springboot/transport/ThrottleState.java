package com.treblle.springboot.transport;

/**
 * Immutable snapshot of the circuit-breaker state. Swapped atomically via
 * {@code AtomicReference} so hot-path reads are lock-free.
 *
 * @param state             current state of the machine
 * @param backoffMillis     current backoff window length in milliseconds
 * @param retryAtEpochMs    epoch millis at which the backoff window expires
 * @param consecutiveFails  number of consecutive failures observed
 * @param probeSuccesses    number of consecutive successful probes
 */
public record ThrottleState(
        State state,
        long backoffMillis,
        long retryAtEpochMs,
        int consecutiveFails,
        int probeSuccesses
) {

    public enum State {
        /** All payloads are sent. */
        NORMAL,
        /** All payloads are dropped until the backoff window expires. */
        BACKING_OFF,
        /** Window expired: exactly one probe request is allowed through. */
        PROBING
    }

    public static ThrottleState normal() {
        return new ThrottleState(State.NORMAL, 0L, 0L, 0, 0);
    }
}
