package com.rayn.taskflow.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A binary min-heap stored in an ArrayList.
 *
 * For the element at index i:
 *   parent      = (i - 1) / 2
 *   left child  = 2i + 1
 *   right child = 2i + 2
 *
 * add / poll / remove are O(log n), peek is O(1).
 * Written from scratch instead of using java.util.PriorityQueue
 * to show how the structure actually works.
 */
public class MinHeap<T> {

    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> cmp;

    public MinHeap(Comparator<? super T> cmp) {
        this.cmp = cmp;
    }

    public void add(T item) {
        items.add(item);
        siftUp(items.size() - 1);
    }

    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("heap is empty");
        return items.get(0);
    }

    public T poll() {
        if (isEmpty()) throw new NoSuchElementException("heap is empty");
        return removeAt(0);
    }

    /**
     * Removes a specific item (found with a linear scan, so O(n) overall).
     * Returns true if it was in the heap.
     */
    public boolean remove(T item) {
        int idx = items.indexOf(item);
        if (idx < 0) return false;
        removeAt(idx);
        return true;
    }

    public int size() { return items.size(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public void clear() { items.clear(); }

    /** Snapshot of the items in sorted order. Doesn't change the heap. O(n log n). */
    public List<T> toSortedList() {
        List<T> copy = new ArrayList<>(items);
        copy.sort(cmp);
        return copy;
    }

    private T removeAt(int idx) {
        int last = items.size() - 1;
        T removed = items.get(idx);
        swap(idx, last);
        items.remove(last);
        if (idx < items.size()) {
            // the element we moved in could need to go either direction
            siftDown(idx);
            siftUp(idx);
        }
        return removed;
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (cmp.compare(items.get(i), items.get(parent)) >= 0) break;
            swap(i, parent);
            i = parent;
        }
    }

    private void siftDown(int i) {
        int n = items.size();
        while (true) {
            int left = 2 * i + 1;
            int right = left + 1;
            int smallest = i;
            if (left < n && cmp.compare(items.get(left), items.get(smallest)) < 0) smallest = left;
            if (right < n && cmp.compare(items.get(right), items.get(smallest)) < 0) smallest = right;
            if (smallest == i) return;
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        T tmp = items.get(a);
        items.set(a, items.get(b));
        items.set(b, tmp);
    }
}
