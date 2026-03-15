# Java Backend Skills Demo

A collection of pure Java demos covering concurrency, design patterns, and data structures. Built with Java 17 and no external dependencies -- intended as a reference for core backend engineering concepts.

## Tech

- Java 17
- No external libraries or frameworks
- Maven for build (optional; plain `javac` works too)

## Project Structure

```
src/main/java/com/demo/skills/
├── concurrency/
│   ├── ProducerConsumer.java      - Producer-consumer using BlockingQueue with AtomicBoolean shutdown
│   └── ParallelAggregator.java    - Parallel service calls via CompletableFuture, thenCombine, and exceptionally
├── patterns/
│   ├── ObserverPattern.java       - Event system with EventBus, typed events, and ConcurrentHashMap handlers
│   ├── StrategyPattern.java       - Payment processing with swappable strategies (CreditCard, BankTransfer, Crypto)
│   └── BuilderPattern.java        - Immutable HttpRequest with fluent Builder and validation
└── datastructures/
    ├── LRUCache.java              - Generic LRU cache backed by LinkedHashMap with hit/miss stats
    └── GraphTraversal.java        - BFS, iterative DFS, recursive DFS, and cycle detection on an adjacency list
```

## What Each Section Demonstrates

### Concurrency

- **ProducerConsumer** -- Coordinating threads through a `BlockingQueue`, with graceful shutdown controlled by an `AtomicBoolean` flag.
- **ParallelAggregator** -- Composing multiple asynchronous service calls using `CompletableFuture.thenCombine` and handling partial failures with `exceptionally`.

### Design Patterns

- **ObserverPattern** -- Decoupled event-driven communication via an `EventBus` that dispatches typed events to registered handlers stored in a `ConcurrentHashMap`.
- **StrategyPattern** -- Swapping payment-processing algorithms at runtime by injecting different strategy implementations (CreditCard, BankTransfer, Crypto).
- **BuilderPattern** -- Constructing immutable `HttpRequest` objects step-by-step with a fluent API, including validation before build.

### Data Structures

- **LRUCache** -- A generic least-recently-used cache that extends `LinkedHashMap` with `removeEldestEntry`, tracking hit and miss counts.
- **GraphTraversal** -- Breadth-first search, iterative depth-first search, recursive depth-first search, and cycle detection over an adjacency-list graph representation.

## How to Run

### Option A: Maven

```bash
mvn compile

# Run a specific demo (replace the class name as needed):
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.ProducerConsumer"
mvn exec:java -Dexec.mainClass="com.demo.skills.concurrency.ParallelAggregator"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.ObserverPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.StrategyPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.patterns.BuilderPattern"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.LRUCache"
mvn exec:java -Dexec.mainClass="com.demo.skills.datastructures.GraphTraversal"
```

### Option B: javac / java

```bash
# Compile all sources
javac -d out src/main/java/com/demo/skills/**/*.java

# Run a specific demo
java -cp out com.demo.skills.concurrency.ProducerConsumer
java -cp out com.demo.skills.concurrency.ParallelAggregator
java -cp out com.demo.skills.patterns.ObserverPattern
java -cp out com.demo.skills.patterns.StrategyPattern
java -cp out com.demo.skills.patterns.BuilderPattern
java -cp out com.demo.skills.datastructures.LRUCache
java -cp out com.demo.skills.datastructures.GraphTraversal
```
