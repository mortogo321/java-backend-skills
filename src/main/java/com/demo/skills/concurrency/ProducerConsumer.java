package com.demo.skills.concurrency;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classic Producer-Consumer pattern using a BlockingQueue.
 *
 * Multiple producer threads generate work items and place them on a shared
 * bounded queue. Multiple consumer threads pull items off the queue and
 * process them. The BlockingQueue handles all synchronization internally.
 */
public class ProducerConsumer {

    private final BlockingQueue<String> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ProducerConsumer(int queueCapacity) {
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
    }

    /**
     * Producer runnable that generates numbered tasks and enqueues them.
     */
    class Producer implements Runnable {
        private final String name;
        private final int itemCount;

        Producer(String name, int itemCount) {
            this.name = name;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    String item = name + "-Task-" + i;
                    queue.put(item); // blocks if queue is full
                    System.out.printf("[%s] Produced: %s (queue size: %d)%n",
                            name, item, queue.size());
                    Thread.sleep(50); // simulate production time
                }
                System.out.printf("[%s] Finished producing.%n", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Interrupted.%n", name);
            }
        }
    }

    /**
     * Consumer runnable that dequeues and processes tasks.
     */
    class Consumer implements Runnable {
        private final String name;

        Consumer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                while (running.get() || !queue.isEmpty()) {
                    String item = queue.poll(200, TimeUnit.MILLISECONDS);
                    if (item != null) {
                        System.out.printf("  [%s] Consumed: %s%n", name, item);
                        Thread.sleep(120); // simulate processing time
                    }
                }
                System.out.printf("  [%s] Shutting down.%n", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("  [%s] Interrupted.%n", name);
            }
        }
    }

    public void stop() {
        running.set(false);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Producer-Consumer Demo ===\n");

        ProducerConsumer pc = new ProducerConsumer(5);

        // Start 2 producers and 3 consumers
        Thread p1 = new Thread(pc.new Producer("P1", 5));
        Thread p2 = new Thread(pc.new Producer("P2", 5));
        Thread c1 = new Thread(pc.new Consumer("C1"));
        Thread c2 = new Thread(pc.new Consumer("C2"));
        Thread c3 = new Thread(pc.new Consumer("C3"));

        c1.start();
        c2.start();
        c3.start();
        p1.start();
        p2.start();

        // Wait for producers to finish
        p1.join();
        p2.join();

        // Signal consumers to stop once queue drains
        pc.stop();

        c1.join();
        c2.join();
        c3.join();

        System.out.println("\nAll producers and consumers finished.");
    }
}
