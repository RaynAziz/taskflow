package com.rayn.taskflow.ds;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {

    @Test
    void pollsInSortedOrder() {
        MinHeap<Integer> heap = new MinHeap<>(Comparator.naturalOrder());
        for (int x : new int[] {5, 3, 8, 1, 9, 2}) heap.add(x);

        List<Integer> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());

        assertEquals(List.of(1, 2, 3, 5, 8, 9), out);
    }

    @Test
    void peekDoesNotRemove() {
        MinHeap<Integer> heap = new MinHeap<>(Comparator.naturalOrder());
        heap.add(4);
        heap.add(2);
        assertEquals(2, heap.peek());
        assertEquals(2, heap.size());
    }

    @Test
    void removeFromMiddleKeepsHeapValid() {
        MinHeap<Integer> heap = new MinHeap<>(Comparator.naturalOrder());
        for (int i = 1; i <= 10; i++) heap.add(i);

        assertTrue(heap.remove(5));
        assertFalse(heap.remove(42));

        List<Integer> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());
        assertEquals(List.of(1, 2, 3, 4, 6, 7, 8, 9, 10), out);
    }

    @Test
    void emptyHeapThrows() {
        MinHeap<String> heap = new MinHeap<>(Comparator.naturalOrder());
        assertThrows(NoSuchElementException.class, heap::peek);
        assertThrows(NoSuchElementException.class, heap::poll);
    }

    @Test
    void matchesSortingOnRandomData() {
        Random rng = new Random(42);
        MinHeap<Integer> heap = new MinHeap<>(Comparator.naturalOrder());
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int x = rng.nextInt(500);
            heap.add(x);
            expected.add(x);
        }
        // remove a few random values along the way too
        for (int i = 0; i < 100; i++) {
            Integer x = expected.get(rng.nextInt(expected.size()));
            assertTrue(heap.remove(x));
            expected.remove(x);
        }
        expected.sort(null);

        List<Integer> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());
        assertEquals(expected, out);
    }
}
