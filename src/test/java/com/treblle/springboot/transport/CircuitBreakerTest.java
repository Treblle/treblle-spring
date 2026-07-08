package com.treblle.springboot.transport;

import com.treblle.springboot.core.TreblleLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerTest {

    private CircuitBreaker breaker() {
        return new CircuitBreaker(new TreblleLogger(false));
    }

    @Test
    void startsInNormalAndAllowsSends() {
        CircuitBreaker cb = breaker();
        assertEquals(ThrottleState.State.NORMAL, cb.currentState());
        assertTrue(cb.tryAcquire());
    }

    @Test
    void failureEntersBackingOffAndDropsPayloads() {
        CircuitBreaker cb = breaker();
        cb.onFailure(null);
        assertEquals(ThrottleState.State.BACKING_OFF, cb.currentState());
        assertFalse(cb.tryAcquire());
    }

    @Test
    void retryAfterIsHonoredAndCappedAt60s() {
        CircuitBreaker cb = breaker();
        cb.onFailure(30L);
        long backoff = cb.currentSnapshot().backoffMillis();
        assertEquals(30_000L, backoff);

        CircuitBreaker cb2 = breaker();
        cb2.onFailure(120L); // over the cap
        assertEquals(60_000L, cb2.currentSnapshot().backoffMillis());
    }

    @Test
    void parsesOnlyDeltaSecondsRetryAfter() {
        assertEquals(30L, CircuitBreaker.parseRetryAfter("30"));
        assertNull(CircuitBreaker.parseRetryAfter("Wed, 21 Oct 2026 07:28:00 GMT"));
        assertNull(CircuitBreaker.parseRetryAfter("not-a-number"));
        assertNull(CircuitBreaker.parseRetryAfter(null));
    }

    @Test
    void exponentialBackoffGrowsWithConsecutiveFailures() {
        CircuitBreaker cb = breaker();
        cb.onFailure(null);
        long first = cb.currentSnapshot().backoffMillis();
        cb.onFailure(null);
        long second = cb.currentSnapshot().backoffMillis();
        // Second window should be roughly double the first (allowing for jitter overlap).
        assertTrue(second > first, "expected backoff to grow: " + first + " -> " + second);
        // Never exceeds the cap plus jitter margin.
        assertTrue(second <= 60_000L);
    }

    @Test
    void probeAllowedAfterWindowExpires() {
        CircuitBreaker cb = breaker();
        cb.onFailure(0L); // 0-second backoff -> window already expired
        assertTrue(cb.tryAcquire(), "probe should be allowed once window expires");
        assertEquals(ThrottleState.State.PROBING, cb.currentState());
        // While one probe is in flight, others are dropped.
        assertFalse(cb.tryAcquire());
    }

    @Test
    void twoSuccessfulProbesRecoverToNormal() {
        CircuitBreaker cb = breaker();
        cb.onFailure(0L);
        assertTrue(cb.tryAcquire()); // probe 1 in flight
        cb.onSuccess();              // one good probe, needs another
        assertEquals(ThrottleState.State.BACKING_OFF, cb.currentState());

        // Force the short window to expire and probe again.
        cb.onFailureWindowExpiredForTest();
        assertTrue(cb.tryAcquire()); // probe 2 in flight
        cb.onSuccess();              // second good probe -> recover
        assertEquals(ThrottleState.State.NORMAL, cb.currentState());
    }

    @Test
    void failedProbeReturnsToBackingOff() {
        CircuitBreaker cb = breaker();
        cb.onFailure(0L);
        assertTrue(cb.tryAcquire()); // probe in flight
        cb.onFailure(null);          // probe failed
        assertEquals(ThrottleState.State.BACKING_OFF, cb.currentState());
    }
}
