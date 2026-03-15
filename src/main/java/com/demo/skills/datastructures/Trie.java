package com.demo.skills.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Trie (prefix tree) supporting insert, search, prefix matching, delete,
 * and autocomplete with a configurable result limit.
 *
 * <p>Time complexities (where <i>m</i> is the word length):
 * <ul>
 *   <li>insert: O(m)</li>
 *   <li>search: O(m)</li>
 *   <li>startsWith: O(m)</li>
 *   <li>delete: O(m)</li>
 *   <li>autocomplete: O(m + k) where k = number of nodes visited under the prefix</li>
 * </ul>
 */
public class Trie {

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
        int prefixCount; // how many words pass through this node
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            node.prefixCount++;
        }
        node.isEndOfWord = true;
    }

    public boolean search(String word) {
        TrieNode node = traverse(word);
        return node != null && node.isEndOfWord;
    }

    public boolean startsWith(String prefix) {
        return traverse(prefix) != null;
    }

    /**
     * Deletes a word from the trie. Returns true if the word existed and was removed.
     * Prunes nodes that are no longer part of any other word.
     */
    public boolean delete(String word) {
        return delete(root, word, 0);
    }

    private boolean delete(TrieNode node, String word, int index) {
        if (index == word.length()) {
            if (!node.isEndOfWord) return false;
            node.isEndOfWord = false;
            return true;
        }

        char c = word.charAt(index);
        TrieNode child = node.children.get(c);
        if (child == null) return false;

        boolean deleted = delete(child, word, index + 1);
        if (deleted) {
            child.prefixCount--;
            if (child.prefixCount == 0) {
                node.children.remove(c);
            }
        }
        return deleted;
    }

    /**
     * Returns up to {@code limit} words that share the given prefix,
     * in lexicographic (DFS) order.
     */
    public List<String> autocomplete(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        TrieNode node = traverse(prefix);
        if (node != null) {
            collectWords(node, new StringBuilder(prefix), results, limit);
        }
        return results;
    }

    private void collectWords(TrieNode node, StringBuilder path, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isEndOfWord) {
            results.add(path.toString());
        }
        // Sort keys for deterministic lexicographic order
        node.children.keySet().stream().sorted().forEach(c -> {
            if (results.size() >= limit) return;
            path.append(c);
            collectWords(node.children.get(c), path, results, limit);
            path.deleteCharAt(path.length() - 1);
        });
    }

    private TrieNode traverse(String s) {
        TrieNode node = root;
        for (char c : s.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return null;
        }
        return node;
    }

    // -------------------------------------------------------------------------
    // Demo
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Trie Demo ===\n");

        Trie trie = new Trie();
        String[] words = {
            "algorithm", "algebra", "alpine", "alpha", "all",
            "binary", "bind", "bit",
            "compile", "compiler", "compute", "concurrent",
            "data", "database", "datastructure"
        };

        System.out.println("Inserting words:");
        for (String w : words) {
            trie.insert(w);
            System.out.println("  + " + w);
        }

        System.out.println("\nSearch:");
        for (String q : new String[]{"algorithm", "algo", "binary", "bit", "xyz"}) {
            System.out.printf("  search(\"%s\") = %s%n", q, trie.search(q));
        }

        System.out.println("\nPrefix check:");
        for (String p : new String[]{"al", "bin", "comp", "xyz"}) {
            System.out.printf("  startsWith(\"%s\") = %s%n", p, trie.startsWith(p));
        }

        System.out.println("\nAutocomplete (limit 5):");
        for (String p : new String[]{"al", "com", "data", "b"}) {
            List<String> results = trie.autocomplete(p, 5);
            System.out.printf("  autocomplete(\"%s\") = %s%n", p, results);
        }

        System.out.println("\nDelete:");
        System.out.printf("  delete(\"alpha\") = %s%n", trie.delete("alpha"));
        System.out.printf("  search(\"alpha\")  = %s  (should be false)%n", trie.search("alpha"));
        System.out.printf("  search(\"alpine\") = %s  (should still be true)%n", trie.search("alpine"));
        System.out.printf("  autocomplete(\"al\") = %s%n", trie.autocomplete("al", 10));
    }
}
