package com.demo.skills.patterns;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A generic Circuit Breaker that wraps unreliable service calls.
 *
 * <h3>State machine:</h3>
 * <pre>
 *   CLOSED  --[failures >= threshold]--> OPEN
 *   OPEN    --[timeout elapsed]-------> HALF_OPEN
 *   HALF_OPEN --[success]--------------> CLOSED
 *   HALF_OPEN --[failure]--------------> OPEN
 * </pre>
 *
 * <h3>Design decisions:</h3>
 * <ul>
 *   <li>Thread-safe via {@link AtomicReference} for state and {@link AtomicInteger}
 *       for counters -- no synchronized blocks needed.</li>
 *   <li>Configurable failure threshold, open-state timeout, and maximum
 *       half-open probe attempts.</li>
 *   <li>Generic {@code <T>} return type so it wraps any {@link Supplier}.</li>
 * </ul>
 *
 * @param <T> the return type of the protected call
 */
public class CircuitBreaker<T> {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);

    private volatile Instant openedAt = Instant.MIN;

    private final int failureThreshold;
    private final Duration openTimeout;
    private final int halfOpenMaxAttempts;

    public CircuitBreaker(int failureThreshold, Duration openTimeout, int halfOpenMaxAttempts) {
        this.failureThreshold = failureThreshold;
        this.openTimeout = openTimeout;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    /**
     * Execute the given supplier through the circuit breaker.
     *
     * @throws CircuitOpenException if the circuit is OPEN and the timeout has not elapsed
     */
    public T execute(Supplier<T> supplier) {
        State currentState = state.get();

        if (currentState == State.OPEN) {
            if (Duration.between(openedAt, Instant.now()).compareTo(openTimeout) >= 0) {
                // Timeout elapsed -- transition to HALF_OPEN to probe
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenAttempts.set(0);
                    System.out.println("  [CircuitBreaker] OPEN -> HALF_OPEN (timeout elapsed, probing)");
                }
            } else {
                throw new CircuitOpenException("Circuit is OPEN -- call rejected");
            }
        }

        // At this point state is CLOSED or HALF_OPEN
        try {
            T result = supplier.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        if (state.get() == State.HALF_OPEN) {
            // Probe succeeded -- close the circuit
            state.set(State.CLOSED);
            failureCount.set(0);
            halfOpenAttempts.set(0);
            System.out.println("  [CircuitBreaker] HALF_OPEN -> CLOSED (probe succeeded)");
        } else {
            failureCount.set(0);
        }
    }

    private void onFailure() {
        if (state.get() == State.HALF_OPEN) {
            int attempts = halfOpenAttempts.incrementAndGet();
            if (attempts >= halfOpenMaxAttempts) {
                trip();
                System.out.println("  [CircuitBreaker] HALF_OPEN -> OPEN (probe failed)");
            }
        } else {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                trip();
                System.out.println("  [CircuitBreaker] CLOSED -> OPEN (threshold reached: " + failures + " failures)");
            }
        }
    }

    private void trip() {
        state.set(State.OPEN);
        openedAt = Instant.now();
    }

    public State getState() {
        return state.get();
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }

    // -------------------------------------------------------------------------
    // Demo
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Circuit Breaker Demo ===\n");

        // Trip after 3 failures, wait 2 seconds before half-open, allow 1 half-open probe
        CircuitBreaker<String> breaker = new CircuitBreaker<>(3, Duration.ofSeconds(2), 1);

        AtomicInteger callCount = new AtomicInteger(0);

        // Simulated flaky service: fails on calls 1-4, succeeds after that
        Supplier<String> flakyService = () -> {
            int n = callCount.incrementAndGet();
            if (n <= 4) {
                throw new RuntimeException("Service failure #" + n);
            }
            return "OK (call #" + n + ")";
        };

        // Phase 1: calls accumulate failures until the circuit opens
        System.out.println("Phase 1: Accumulating failures...");
        for (int i = 0; i < 5; i++) {
            try {
                String result = breaker.execute(flakyService);
                System.out.println("  Result: " + result + "  | state=" + breaker.getState());
            } catch (CircuitOpenException e) {
                System.out.println("  REJECTED: " + e.getMessage() + "  | state=" + breaker.getState());
            } catch (RuntimeException e) {
                System.out.println("  FAILED: " + e.getMessage() + "  | state=" + breaker.getState());
            }
        }

        // Phase 2: wait for timeout, then circuit should transition to HALF_OPEN
        System.out.println("\nPhase 2: Waiting for open timeout (2s)...");
        Thread.sleep(2200);

        // Phase 3: next call is a half-open probe -- the flaky service is still failing (call #4)
        // but call #5 will succeed
        System.out.println("\nPhase 3: Probing (half-open)...");
        for (int i = 0; i < 3; i++) {
            try {
                String result = breaker.execute(flakyService);
                System.out.println("  Result: " + result + "  | state=" + breaker.getState());
            } catch (CircuitOpenException e) {
                System.out.println("  REJECTED: " + e.getMessage() + "  | state=" + breaker.getState());
            } catch (RuntimeException e) {
                System.out.println("  FAILED: " + e.getMessage() + "  | state=" + breaker.getState());
            }
        }

        // Phase 4: after timeout, probe again -- now the service is healthy
        System.out.println("\nPhase 4: Waiting again then probing with healthy service...");
        Thread.sleep(2200);

        for (int i = 0; i < 3; i++) {
            try {
                String result = breaker.execute(flakyService);
                System.out.println("  Result: " + result + "  | state=" + breaker.getState());
            } catch (CircuitOpenException e) {
                System.out.println("  REJECTED: " + e.getMessage() + "  | state=" + breaker.getState());
            } catch (RuntimeException e) {
                System.out.println("  FAILED: " + e.getMessage() + "  | state=" + breaker.getState());
            }
        }
    }
}
