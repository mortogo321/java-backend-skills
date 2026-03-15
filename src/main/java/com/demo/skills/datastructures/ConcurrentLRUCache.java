package com.demo.skills.datastructures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe LRU (Least Recently Used) cache.
 *
 * <h3>Implementation strategy:</h3>
 * Uses a {@link ConcurrentHashMap} for O(1) lookups combined with a
 * hand-rolled doubly-linked list for O(1) recency tracking and eviction.
 * A {@link ReentrantReadWriteLock} protects linked-list mutations while
 * allowing concurrent reads where possible.
 *
 * <h3>Why not just Collections.synchronizedMap + LinkedHashMap?</h3>
 * That approach serializes every operation behind a single monitor.
 * This implementation allows concurrent {@code get()} calls to proceed
 * under the read lock when the entry exists and only escalates to a
 * write lock for structural modifications (promotion, insertion, eviction).
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ConcurrentLRUCache<K, V> {

    // Doubly-linked list node
    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Sentinel head/tail simplify edge cases
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger misses = new AtomicInteger();

    public ConcurrentLRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        head.next = tail;
        tail.prev = head;
    }

    public Optional<V> get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        hits.incrementAndGet();
        // Move to front (most recently used)
        rwLock.writeLock().lock();
        try {
            moveToFront(node);
        } finally {
            rwLock.writeLock().unlock();
        }
        return Optional.of(node.value);
    }

    public void put(K key, V value) {
        Node<K, V> existing = map.get(key);

        rwLock.writeLock().lock();
        try {
            if (existing != null) {
                existing.value = value;
                moveToFront(existing);
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                addToFront(newNode);
                map.put(key, newNode);

                if (map.size() > capacity) {
                    Node<K, V> lru = tail.prev;
                    removeNode(lru);
                    map.remove(lru.key);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int size() {
        return map.size();
    }

    public int getHits() { return hits.get(); }
    public int getMisses() { return misses.get(); }

    // --- Linked list operations (caller must hold write lock) ---

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToFront(Node<K, V> node) {
        removeNode(node);
        addToFront(node);
    }

    // -------------------------------------------------------------------------
    // Demo
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ConcurrentLRUCache Demo ===\n");

        ConcurrentLRUCache<String, Integer> cache = new ConcurrentLRUCache<>(3);

        // Basic operations
        System.out.println("--- Single-threaded basics ---");
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        System.out.printf("get(a)=%s, get(b)=%s, get(c)=%s%n",
                cache.get("a"), cache.get("b"), cache.get("c"));

        cache.put("d", 4); // should evict least recently used
        System.out.printf("After put(d,4): get(a)=%s (evicted), get(d)=%s%n",
                cache.get("a"), cache.get("d"));
        System.out.printf("Size: %d, Hits: %d, Misses: %d%n%n",
                cache.size(), cache.getHits(), cache.getMisses());

        // Multi-threaded stress test
        System.out.println("--- Multi-threaded stress test (8 threads, 10K ops each) ---");
        ConcurrentLRUCache<Integer, Integer> stressCache = new ConcurrentLRUCache<>(100);
        int threadCount = 8;
        int opsPerThread = 10_000;
        AtomicInteger errors = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads.add(Thread.ofVirtual().name("worker-" + threadId).start(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int key = (threadId * opsPerThread + i) % 200;
                        stressCache.put(key, key * 10);
                        stressCache.get(key);
                        // Also read a random-ish key that may or may not exist
                        stressCache.get((key + 97) % 200);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            }));
        }

        for (Thread t : threads) t.join();

        System.out.printf("Completed %,d total operations across %d threads%n",
                threadCount * opsPerThread * 3L, threadCount);
        System.out.printf("Cache size: %d (capacity: 100)%n", stressCache.size());
        System.out.printf("Hits: %,d, Misses: %,d%n", stressCache.getHits(), stressCache.getMisses());
        System.out.printf("Errors: %d%n", errors.get());
    }
}
