package com.rayn.taskflow.ds;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrieTest {

    @Test
    void findsByPrefixOfAnyWord() {
        Trie trie = new Trie();
        trie.insert("Fix kitchen plumbing", 1);
        trie.insert("Call the plumber", 2);
        trie.insert("Gym", 3);

        assertEquals(Set.of(1, 2), trie.search("pl"));
        assertEquals(Set.of(1), trie.search("kit"));
        assertEquals(Set.of(3), trie.search("GYM"));
        assertTrue(trie.search("xyz").isEmpty());
    }

    @Test
    void blankSearchReturnsNothing() {
        Trie trie = new Trie();
        trie.insert("anything", 1);
        assertTrue(trie.search("  ").isEmpty());
    }

    @Test
    void removeOnlyAffectsThatId() {
        Trie trie = new Trie();
        trie.insert("buy groceries", 1);
        trie.insert("buy stamps", 2);

        trie.remove("buy groceries", 1);

        assertEquals(Set.of(2), trie.search("buy"));
        assertTrue(trie.search("groc").isEmpty());
    }

    @Test
    void ignoresPunctuation() {
        Trie trie = new Trie();
        trie.insert("email landlord (re: lease!)", 7);
        assertEquals(Set.of(7), trie.search("lea"));
        assertEquals(Set.of(7), trie.search("re"));
    }
}
