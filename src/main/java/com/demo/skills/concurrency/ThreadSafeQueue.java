package com.demo.skills.concurrency;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A bounded, thread-safe blocking queue built from scratch using a
 * {@link ReentrantLock} with two {@link Condition} variables.
 *
 * <p>This is the classic implementation that interviewers and system-design
 * discussions expect you to know. The JDK's {@code ArrayBlockingQueue} uses
 * the same approach internally.
 *
 * <h3>Key concurrency concepts demonstrated:</h3>
 * <ul>
 *   <li>ReentrantLock as a more flexible alternative to {@code synchronized}</li>
 *   <li>Condition variables for wait/signal (replacing Object.wait/notify)</li>
 *   <li>Spurious-wakeup protection via {@code while} loops around {@code await()}</li>
 *   <li>Lock acquisition in try/finally to guarantee release</li>
 * </ul>
 *
 * @param <T> the type of elements held in this queue
 */
public class ThreadSafeQueue<T> {

    private final LinkedList<T> buffer;
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public ThreadSafeQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.buffer = new LinkedList<>();
    }

    /**
     * Inserts an element, blocking if the queue is full.
     */
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // Guard against spurious wakeups with a while-loop
            while (buffer.size() == capacity) {
                notFull.await();
            }
            buffer.addLast(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the head element, blocking if the queue is empty.
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                notEmpty.await();
            }
            T item = buffer.removeFirst();
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    // -------------------------------------------------------------------------
    // Demo
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ThreadSafeQueue Demo ===\n");

        final int CAPACITY = 5;
        final int ITEMS_PER_PRODUCER = 10;
        final int PRODUCERS = 3;
        final int CONSUMERS = 2;
        final int TOTAL_ITEMS = PRODUCERS * ITEMS_PER_PRODUCER;

        ThreadSafeQueue<String> queue = new ThreadSafeQueue<>(CAPACITY);

        // Producers
        Thread[] producers = new Thread[PRODUCERS];
        for (int p = 0; p < PRODUCERS; p++) {
            final int id = p;
            producers[p] = Thread.ofVirtual().name("producer-" + id).start(() -> {
                try {
                    for (int i = 0; i < ITEMS_PER_PRODUCER; i++) {
                        String item = "P" + id + "-item" + i;
                        queue.put(item);
                        System.out.printf("  [%s] put: %s  (queue size ≈ %d)%n",
                                Thread.currentThread().getName(), item, queue.size());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Consumers
        java.util.concurrent.atomic.AtomicInteger consumed = new java.util.concurrent.atomic.AtomicInteger();
        Thread[] consumers = new Thread[CONSUMERS];
        for (int c = 0; c < CONSUMERS; c++) {
            final int id = c;
            consumers[c] = Thread.ofVirtual().name("consumer-" + id).start(() -> {
                try {
                    while (consumed.get() < TOTAL_ITEMS) {
                        String item = queue.take();
                        int count = consumed.incrementAndGet();
                        System.out.printf("  [%s] took: %s  (total consumed: %d/%d)%n",
                                Thread.currentThread().getName(), item, count, TOTAL_ITEMS);
                        if (count >= TOTAL_ITEMS) break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for producers to finish
        for (Thread t : producers) t.join();
        System.out.println("\nAll producers finished.");

        // Wait for consumers to finish
        for (Thread t : consumers) t.join(3000);
        System.out.printf("%nAll items consumed. Final queue size: %d%n", queue.size());
    }
}
