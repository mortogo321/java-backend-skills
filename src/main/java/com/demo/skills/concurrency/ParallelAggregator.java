package com.demo.skills.concurrency;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates CompletableFuture to call multiple "services" in parallel
 * and aggregate the results into a single response object.
 */
public class ParallelAggregator {

    record UserProfile(String name, int age, String email) {}
    record OrderHistory(List<String> orders) {}
    record Recommendations(List<String> items) {}

    /**
     * Aggregated response combining data from all services.
     */
    record DashboardData(UserProfile profile, OrderHistory orders, Recommendations recommendations) {
        void print() {
            System.out.println("  Profile: " + profile.name() + ", age " + profile.age()
                    + ", " + profile.email());
            System.out.println("  Orders:  " + orders.orders());
            System.out.println("  Recommended: " + recommendations.items());
        }
    }

    // Simulated service calls with artificial latency

    private static CompletableFuture<UserProfile> fetchUserProfile(String userId,
                                                                    ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency("UserService", 300);
            return new UserProfile("Alice", 29, "alice@example.com");
        }, executor);
    }

    private static CompletableFuture<OrderHistory> fetchOrderHistory(String userId,
                                                                      ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency("OrderService", 500);
            return new OrderHistory(List.of("ORD-1001", "ORD-1042", "ORD-1099"));
        }, executor);
    }

    private static CompletableFuture<Recommendations> fetchRecommendations(String userId,
                                                                            ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency("RecommendationService", 400);
            return new Recommendations(List.of("Wireless Mouse", "USB-C Hub", "Monitor Stand"));
        }, executor);
    }

    /**
     * Aggregates results from three parallel service calls into a single dashboard.
     */
    public static CompletableFuture<DashboardData> buildDashboard(String userId,
                                                                    ExecutorService executor) {
        CompletableFuture<UserProfile> profileFuture = fetchUserProfile(userId, executor);
        CompletableFuture<OrderHistory> ordersFuture = fetchOrderHistory(userId, executor);
        CompletableFuture<Recommendations> recoFuture = fetchRecommendations(userId, executor);

        return profileFuture.thenCombine(ordersFuture, (profile, orders) ->
                Map.entry(profile, orders)
        ).thenCombine(recoFuture, (pair, recs) ->
                new DashboardData(pair.getKey(), pair.getValue(), recs)
        );
    }

    /**
     * Demonstrates error handling: one of the futures fails, and we provide a fallback.
     */
    public static CompletableFuture<String> fetchWithFallback(ExecutorService executor) {
        CompletableFuture<String> unreliable = CompletableFuture.supplyAsync(() -> {
            simulateLatency("UnreliableService", 200);
            throw new RuntimeException("Service timeout!");
        }, executor);

        return unreliable
                .exceptionally(ex -> {
                    System.out.println("  Caught: " + ex.getMessage() + " -> using fallback");
                    return "fallback-data";
                });
    }

    private static void simulateLatency(String serviceName, long millis) {
        System.out.printf("  [%s] Calling %s...%n", Thread.currentThread().getName(), serviceName);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("  [%s] %s responded.%n", Thread.currentThread().getName(), serviceName);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Parallel Aggregator Demo ===\n");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // 1. Parallel aggregation
        System.out.println("1) Building dashboard (3 parallel calls)...");
        long start = System.currentTimeMillis();

        DashboardData dashboard = buildDashboard("user-42", executor)
                .get(5, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("\n  Dashboard assembled in " + elapsed + "ms (sequential would be ~1200ms):");
        dashboard.print();

        // 2. Error handling with fallback
        System.out.println("\n2) Calling unreliable service with fallback...");
        String result = fetchWithFallback(executor).get(5, TimeUnit.SECONDS);
        System.out.println("  Result: " + result);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\nDone.");
    }
}
