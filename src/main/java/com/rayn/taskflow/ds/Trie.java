package com.rayn.taskflow.ds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A prefix tree used for search-as-you-type.
 *
 * Each word is inserted letter by letter, and every node along the path
 * remembers which task ids contain a word with that prefix. So looking up
 * a prefix of length k is O(k), no matter how many tasks there are.
 *
 * Words are stored lowercase so search is case-insensitive.
 */
public class Trie {

    private static class Node {
        // TreeMap keeps children in alphabetical order (nice for debugging/printing)
        final Map<Character, Node> children = new TreeMap<>();
        final Set<Integer> ids = new HashSet<>();
    }

    private final Node root = new Node();

    /** Indexes every word of the text under the given id. */
    public void insert(String text, int id) {
        for (String word : splitWords(text)) {
            Node node = root;
            for (char c : word.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new Node());
                node.ids.add(id);
            }
        }
    }

    /** Removes the id from every node for the words in text, and prunes empty branches. */
    public void remove(String text, int id) {
        for (String word : splitWords(text)) {
            removeWord(root, word, 0, id);
        }
    }

    /** Returns ids of everything that has a word starting with prefix. */
    public Set<Integer> search(String prefix) {
        String p = prefix.trim().toLowerCase();
        if (p.isEmpty()) return new HashSet<>();
        Node node = root;
        for (char c : p.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return new HashSet<>();
        }
        return new HashSet<>(node.ids);
    }

    public void clear() {
        root.children.clear();
        root.ids.clear();
    }

    // returns true if this node ended up empty, so the parent can delete it
    private boolean removeWord(Node node, String word, int depth, int id) {
        if (depth == word.length()) return node.ids.isEmpty() && node.children.isEmpty();
        char c = word.charAt(depth);
        Node child = node.children.get(c);
        if (child == null) return false;
        child.ids.remove(id);
        if (removeWord(child, word, depth + 1, id) && child.ids.isEmpty()) {
            node.children.remove(c);
        }
        return node.ids.isEmpty() && node.children.isEmpty();
    }

    static List<String> splitWords(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) return words;
        for (String w : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!w.isEmpty()) words.add(w);
        }
        return words;
    }
}
