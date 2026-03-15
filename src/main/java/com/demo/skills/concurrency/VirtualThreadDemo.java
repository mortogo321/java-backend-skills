package com.demo.skills.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates Java 21 virtual threads vs platform threads.
 *
 * Virtual threads (JEP 444) are lightweight threads managed by the JVM rather
 * than the OS. They make it practical to create millions of concurrent tasks
 * that spend most of their time blocking on I/O, without the memory overhead
 * of one OS thread per task.
 *
 * This demo benchmarks:
 *   1. 10,000 virtual threads each doing simulated I/O (Thread.sleep 100ms)
 *   2. 10,000 tasks on a fixed pool of 100 platform threads doing the same work
 *
 * Expected result: virtual threads finish in roughly 100-200ms wall-clock time
 * (all 10K sleep concurrently), while platform threads take ~10 seconds
 * (100 threads * 100 batches of 100ms each).
 */
public class VirtualThreadDemo {

    private static final int TASK_COUNT = 10_000;
    private static final int PLATFORM_POOL_SIZE = 100;
    private static final Duration SIMULATED_IO = Duration.ofMillis(100);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Virtual Threads vs Platform Threads Benchmark ===\n");

        long virtualTime = benchmarkVirtualThreads();
        long platformTime = benchmarkPlatformThreads();

        System.out.println("\n--- Results ---");
        System.out.printf("Virtual  threads (%,d tasks): %,d ms%n", TASK_COUNT, virtualTime);
        System.out.printf("Platform threads (%,d pool, %,d tasks): %,d ms%n",
                PLATFORM_POOL_SIZE, TASK_COUNT, platformTime);
        System.out.printf("Speedup: %.1fx%n", (double) platformTime / virtualTime);

        System.out.println("\n=== Structured Concurrency with ExecutorService ===\n");
        structuredConcurrencyDemo();
    }

    /**
     * Spawns 10,000 virtual threads. Each virtual thread is backed by a
     * carrier (platform) thread only while it is actively running; during
     * Thread.sleep the carrier is released to serve other virtual threads.
     */
    private static long benchmarkVirtualThreads() throws InterruptedException {
        AtomicInteger completed = new AtomicInteger();

        Instant start = Instant.now();

        // try-with-resources ensures all tasks complete before we proceed
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    simulateIO();
                    completed.incrementAndGet();
                });
            }
        } // executor.close() waits for all tasks

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("[Virtual]  Completed %,d tasks in %,d ms%n", completed.get(), elapsed);
        return elapsed;
    }

    /**
     * Runs the same 10,000 tasks on a fixed thread pool of 100 platform threads.
     * Because each task sleeps for 100ms and only 100 can run concurrently,
     * the total wall-clock time is roughly (10,000 / 100) * 100ms = 10 seconds.
     */
    private static long benchmarkPlatformThreads() throws InterruptedException {
        AtomicInteger completed = new AtomicInteger();

        Instant start = Instant.now();

        try (ExecutorService executor = Executors.newFixedThreadPool(PLATFORM_POOL_SIZE)) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    simulateIO();
                    completed.incrementAndGet();
                });
            }
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("[Platform] Completed %,d tasks in %,d ms%n", completed.get(), elapsed);
        return elapsed;
    }

    /**
     * Demonstrates structured concurrency: the ExecutorService used with
     * try-with-resources guarantees that all submitted tasks finish before
     * the block exits. This replaces the old pattern of calling
     * executor.shutdown() + awaitTermination().
     *
     * Here we simulate fetching data from three independent services
     * concurrently, then combine the results.
     */
    private static void structuredConcurrencyDemo() throws Exception {
        record ServiceResult(String service, String data, long latencyMs) {}

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var userFuture = executor.submit(() -> {
                Thread.sleep(80);
                return new ServiceResult("UserService", "user=alice", 80);
            });

            var orderFuture = executor.submit(() -> {
                Thread.sleep(120);
                return new ServiceResult("OrderService", "orders=5", 120);
            });

            var inventoryFuture = executor.submit(() -> {
                Thread.sleep(50);
                return new ServiceResult("InventoryService", "items=42", 50);
            });

            // All three run concurrently on virtual threads; total wait ≈ max(80,120,50) ms
            ServiceResult user = userFuture.get();
            ServiceResult order = orderFuture.get();
            ServiceResult inventory = inventoryFuture.get();

            System.out.println("Aggregated results (all fetched concurrently on virtual threads):");
            System.out.printf("  %s -> %s (%dms)%n", user.service(), user.data(), user.latencyMs());
            System.out.printf("  %s -> %s (%dms)%n", order.service(), order.data(), order.latencyMs());
            System.out.printf("  %s -> %s (%dms)%n", inventory.service(), inventory.data(), inventory.latencyMs());
        }
    }

    private static void simulateIO() {
        try {
            Thread.sleep(SIMULATED_IO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
