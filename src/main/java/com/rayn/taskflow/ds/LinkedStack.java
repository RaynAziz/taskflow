package com.rayn.taskflow.ds;

import java.util.NoSuchElementException;

/**
 * A stack built on a singly linked list. push/pop/peek are all O(1).
 * Used to keep the history of actions for undo.
 *
 * It has an optional max size so the undo history can't grow forever;
 * when it's full, the oldest entry (the bottom) gets dropped.
 */
public class LinkedStack<T> {

    private static class Node<T> {
        final T value;
        Node<T> next;
        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;
    private final int capacity;

    public LinkedStack() {
        this(Integer.MAX_VALUE);
    }

    public LinkedStack(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public void push(T value) {
        top = new Node<>(value, top);
        size++;
        if (size > capacity) dropBottom();
    }

    public T pop() {
        if (top == null) throw new NoSuchElementException("stack is empty");
        T value = top.value;
        top = top.next;
        size--;
        return value;
    }

    public T peek() {
        if (top == null) throw new NoSuchElementException("stack is empty");
        return top.value;
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public void clear() {
        top = null;
        size = 0;
    }

    // O(n), but it only happens once the stack is at capacity
    private void dropBottom() {
        if (size == 1) { // capacity is at least 1, so this can't really happen, but just in case
            return;
        }
        Node<T> cur = top;
        while (cur.next.next != null) {
            cur = cur.next;
        }
        cur.next = null;
        size--;
    }
}
