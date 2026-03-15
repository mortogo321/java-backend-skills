package com.demo.skills.datastructures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic LRU (Least Recently Used) cache backed by LinkedHashMap.
 *
 * LinkedHashMap with access-order iteration automatically moves accessed
 * entries to the tail. By overriding removeEldestEntry, the map evicts
 * the least-recently-used entry when capacity is exceeded.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LRUCache<K, V> {

    private final int capacity;
    private final LinkedHashMap<K, V> map;
    private int hits;
    private int misses;

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        // accessOrder=true makes LinkedHashMap reorder on get()
        this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                boolean evict = size() > LRUCache.this.capacity;
                if (evict) {
                    System.out.printf("  [Evicting] %s -> %s%n", eldest.getKey(), eldest.getValue());
                }
                return evict;
            }
        };
    }

    public void put(K key, V value) {
        map.put(key, value);
    }

    public Optional<V> get(K key) {
        V value = map.get(key);
        if (value != null) {
            hits++;
            return Optional.of(value);
        }
        misses++;
        return Optional.empty();
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public void printStats() {
        int total = hits + misses;
        double hitRate = total == 0 ? 0 : (100.0 * hits / total);
        System.out.printf("  Cache stats: %d hits, %d misses, %.1f%% hit rate, %d/%d entries%n",
                hits, misses, hitRate, map.size(), capacity);
    }

    public void printContents() {
        System.out.print("  Contents (LRU -> MRU): ");
        map.forEach((k, v) -> System.out.printf("[%s=%s] ", k, v));
        System.out.println();
    }

    // --- Demo ---

    public static void main(String[] args) {
        System.out.println("=== LRU Cache Demo ===\n");

        LRUCache<String, String> cache = new LRUCache<>(3);

        // Fill the cache
        System.out.println("1) Inserting 3 entries:");
        cache.put("a", "Alpha");
        cache.put("b", "Bravo");
        cache.put("c", "Charlie");
        cache.printContents();

        // Access "a" to make it recently used
        System.out.println("\n2) Accessing 'a' (moves to MRU):");
        cache.get("a");
        cache.printContents();

        // Insert "d" -> should evict "b" (now LRU)
        System.out.println("\n3) Inserting 'd' (should evict 'b'):");
        cache.put("d", "Delta");
        cache.printContents();

        // Try to get evicted key
        System.out.println("\n4) Looking up evicted key 'b':");
        Optional<String> result = cache.get("b");
        System.out.println("  get('b') = " + result.orElse("MISS"));

        // More operations
        System.out.println("\n5) More lookups:");
        cache.get("c");
        System.out.println("  get('c') = " + cache.get("c").orElse("MISS"));
        System.out.println("  get('x') = " + cache.get("x").orElse("MISS"));
        System.out.println("  get('a') = " + cache.get("a").orElse("MISS"));

        System.out.println("\n6) Final state:");
        cache.printContents();
        cache.printStats();

        System.out.println("\nDone.");
    }
}
