# Java Backend Skills Demo

A collection of pure Java demos covering concurrency, design patterns, and data structures. Built with Java 21 and no external dependencies -- intended as a reference for core backend engineering concepts.

## Tech

- Java 21
- No external libraries or frameworks
- Maven for build (optional; plain `javac` works too)

## Project Structure

```
src/main/java/com/demo/skills/
├── concurrency/
│   ├── ProducerConsumer.java      - Producer-consumer using BlockingQueue with AtomicBoolean shutdown
│   ├── ParallelAggregator.java    - Parallel service calls via CompletableFuture, thenCombine, and exceptionally
│   ├── VirtualThreadDemo.java     - Java 21 virtual threads vs platform threads benchmark
│   └── ThreadSafeQueue.java       - Bounded blocking queue from scratch with ReentrantLock and Conditions
├── patterns/
│   ├── ObserverPattern.java       - Event system with EventBus, typed events, and ConcurrentHashMap handlers
│   ├── StrategyPattern.java       - Payment processing with swappable strategies (CreditCard, BankTransfer, Crypto)
│   ├── BuilderPattern.java        - Immutable HttpRequest with fluent Builder and validation
│   └── CircuitBreaker.java        - Circuit breaker (CLOSED/OPEN/HALF_OPEN) with configurable thresholds
└── datastructures/
    ├── LRUCache.java              - Generic LRU cache backed by LinkedHashMap with hit/miss stats
    ├── GraphTraversal.java        - BFS, iterative DFS, recursive DFS, and cycle detection on an adjacency list
    ├── Trie.java                  - Prefix tree with insert, search, delete, and autocomplete
    └── ConcurrentLRUCache.java    - Thread-safe LRU cache with ConcurrentHashMap and ReentrantReadWriteLock
```

## What Each Section Demonstrates

### Concurrency

- **ProducerConsumer** -- Coordinating threads through a `BlockingQueue`, with graceful shutdown controlled by an `AtomicBoolean` flag.
- **ParallelAggregator** -- Composing multiple asynchronous service calls using `CompletableFuture.thenCombine` and handling partial failures with `exceptionally`.
- **VirtualThreadDemo** -- Benchmarking Java 21 virtual threads (10,000 concurrent tasks) against a fixed platform thread pool (100 threads). Demonstrates structured concurrency with `ExecutorService` in try-with-resources.
- **ThreadSafeQueue** -- A bounded blocking queue implemented from scratch using `ReentrantLock` with `Condition` variables (`notFull`, `notEmpty`), demonstrating the same approach used internally by `ArrayBlockingQueue`.

### Design Patterns

- **ObserverPattern** -- Decoupled event-driven communication via an `EventBus` that dispatches typed events to registered handlers stored in a `ConcurrentHashMap`.
- **StrategyPattern** -- Swapping payment-processing algorithms at runtime by injecting different strategy implementations (CreditCard, BankTransfer, Crypto).
- **BuilderPattern** -- Constructing immutable `HttpRequest` objects step-by-step with a fluent API, including validation before build.
- **CircuitBreaker** -- A generic, thread-safe circuit breaker wrapping unreliable `Supplier<T>` calls. Transitions through CLOSED, OPEN, and HALF_OPEN states with configurable failure thresholds, timeout durations, and half-open probe limits. Uses `AtomicReference` for lock-free state management.

### Data Structures

- **LRUCache** -- A generic least-recently-used cache that extends `LinkedHashMap` with `removeEldestEntry`, tracking hit and miss counts.
- **GraphTraversal** -- Breadth-first search, iterative depth-first search, recursive depth-first search, and cycle detection over an adjacency-list graph representation.
- **Trie** -- A prefix tree supporting `insert`, `search`, `startsWith`, `delete`, and `autocomplete(prefix, limit)`. Maintains prefix counts for efficient pruning on delete.
- **ConcurrentLRUCache** -- A thread-safe LRU cache using `ConcurrentHashMap` for O(1) lookups and a hand-rolled doubly-linked list protected by a `ReentrantReadWriteLock` for O(1) recency tracking and eviction.

## How to Run

### Option A: Maven

```bash
mvn compile

# Run a specific demo (replace the class name as needed):
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.ProducerConsumer"
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.ParallelAggregator"
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.VirtualThreadDemo"
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.ThreadSafeQueue"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.ObserverPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.StrategyPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.BuilderPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.CircuitBreaker"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.LRUCache"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.GraphTraversal"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.Trie"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.ConcurrentLRUCache"
```

### Option B: javac / java

```bash
# Compile all sources
javac -d out src/main/java/com/demo/skills/**/*.java

# Run a specific demo
java -cp out com.demo.skills.concurrency.ProducerConsumer
java -cp out com.demo.skills.concurrency.ParallelAggregator
java -cp out com.demo.skills.concurrency.VirtualThreadDemo
java -cp out com.demo.skills.concurrency.ThreadSafeQueue
java -cp out com.demo.skills.patterns.ObserverPattern
java -cp out com.demo.skills.patterns.StrategyPattern
java -cp out com.demo.skills.patterns.BuilderPattern
java -cp out com.demo.skills.patterns.CircuitBreaker
java -cp out com.demo.skills.datastructures.LRUCache
java -cp out com.demo.skills.datastructures.GraphTraversal
java -cp out com.demo.skills.datastructures.Trie
java -cp out com.demo.skills.datastructures.ConcurrentLRUCache
```
