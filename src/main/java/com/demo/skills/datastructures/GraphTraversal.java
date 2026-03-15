package com.demo.skills.datastructures;

import java.util.*;

/**
 * Graph represented as an adjacency list with BFS and DFS traversal.
 *
 * Supports both directed and undirected graphs. Demonstrates iterative BFS
 * (using a queue) and both recursive and iterative DFS (using a stack).
 */
public class GraphTraversal {

    private final Map<String, List<String>> adjacency = new LinkedHashMap<>();
    private final boolean directed;

    public GraphTraversal(boolean directed) {
        this.directed = directed;
    }

    public void addVertex(String vertex) {
        adjacency.putIfAbsent(vertex, new ArrayList<>());
    }

    public void addEdge(String from, String to) {
        addVertex(from);
        addVertex(to);
        adjacency.get(from).add(to);
        if (!directed) {
            adjacency.get(to).add(from);
        }
    }

    /**
     * Breadth-First Search: explores neighbors level by level.
     * Returns the visit order.
     */
    public List<String> bfs(String start) {
        List<String> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        if (!adjacency.containsKey(start)) return visited;

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            visited.add(current);

            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                if (seen.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }

    /**
     * Depth-First Search (iterative): explores as deep as possible first.
     * Returns the visit order.
     */
    public List<String> dfsIterative(String start) {
        List<String> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();

        if (!adjacency.containsKey(start)) return visited;

        stack.push(start);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!seen.add(current)) continue;

            visited.add(current);

            // Push neighbors in reverse order so left-most is visited first
            List<String> neighbors = adjacency.getOrDefault(current, List.of());
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                if (!seen.contains(neighbors.get(i))) {
                    stack.push(neighbors.get(i));
                }
            }
        }
        return visited;
    }

    /**
     * DFS (recursive) with path tracking: finds a path between start and target.
     */
    public Optional<List<String>> findPath(String start, String target) {
        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();
        if (dfsRecursive(start, target, visited, path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    private boolean dfsRecursive(String current, String target,
                                  Set<String> visited, List<String> path) {
        visited.add(current);
        path.add(current);

        if (current.equals(target)) return true;

        for (String neighbor : adjacency.getOrDefault(current, List.of())) {
            if (!visited.contains(neighbor)) {
                if (dfsRecursive(neighbor, target, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(path.size() - 1); // backtrack
        return false;
    }

    /**
     * Detects if the graph has a cycle (for directed graphs).
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>(); // nodes in current recursion stack

        for (String vertex : adjacency.keySet()) {
            if (!visited.contains(vertex)) {
                if (hasCycleDfs(vertex, visited, inStack)) return true;
            }
        }
        return false;
    }

    private boolean hasCycleDfs(String current, Set<String> visited, Set<String> inStack) {
        visited.add(current);
        inStack.add(current);

        for (String neighbor : adjacency.getOrDefault(current, List.of())) {
            if (inStack.contains(neighbor)) return true;
            if (!visited.contains(neighbor) && hasCycleDfs(neighbor, visited, inStack)) return true;
        }

        inStack.remove(current);
        return false;
    }

    public void printGraph() {
        adjacency.forEach((vertex, neighbors) ->
                System.out.printf("  %s -> %s%n", vertex, neighbors));
    }

    // --- Demo ---

    public static void main(String[] args) {
        System.out.println("=== Graph Traversal Demo ===\n");

        // Build a directed graph
        //   A -> B -> D -> F
        //   A -> C -> E -> F
        //   B -> E
        GraphTraversal graph = new GraphTraversal(true);
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("B", "E");
        graph.addEdge("C", "E");
        graph.addEdge("D", "F");
        graph.addEdge("E", "F");

        System.out.println("Graph (directed):");
        graph.printGraph();

        System.out.println("\n1) BFS from A: " + graph.bfs("A"));
        System.out.println("2) DFS from A: " + graph.dfsIterative("A"));

        // Path finding
        Optional<List<String>> path = graph.findPath("A", "F");
        System.out.println("3) Path A -> F: " + path.orElse(List.of("no path")));

        Optional<List<String>> noPath = graph.findPath("F", "A");
        System.out.println("4) Path F -> A: " + noPath.orElse(List.of("no path")));

        // Cycle detection
        System.out.println("5) Has cycle: " + graph.hasCycle());

        // Add a cycle and re-check
        graph.addEdge("F", "A");
        System.out.println("6) After adding F->A, has cycle: " + graph.hasCycle());

        // Undirected graph example
        System.out.println("\n--- Undirected graph ---");
        GraphTraversal undirected = new GraphTraversal(false);
        undirected.addEdge("1", "2");
        undirected.addEdge("1", "3");
        undirected.addEdge("2", "4");
        undirected.addEdge("3", "4");
        undirected.addEdge("4", "5");

        undirected.printGraph();
        System.out.println("BFS from 1: " + undirected.bfs("1"));
        System.out.println("DFS from 1: " + undirected.dfsIterative("1"));

        System.out.println("\nDone.");
    }
}
