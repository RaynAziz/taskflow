package com.rayn.taskflow.ds;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class LinkedStackTest {

    @Test
    void lastInFirstOut() {
        LinkedStack<String> stack = new LinkedStack<>();
        stack.push("a");
        stack.push("b");
        stack.push("c");
        assertEquals("c", stack.pop());
        assertEquals("b", stack.peek());
        assertEquals(2, stack.size());
    }

    @Test
    void dropsOldestWhenFull() {
        LinkedStack<Integer> stack = new LinkedStack<>(3);
        for (int i = 1; i <= 5; i++) stack.push(i);

        assertEquals(3, stack.size());
        assertEquals(5, stack.pop());
        assertEquals(4, stack.pop());
        assertEquals(3, stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void capacityOfOneStillWorks() {
        LinkedStack<Integer> stack = new LinkedStack<>(1);
        stack.push(1);
        stack.push(2);
        assertEquals(1, stack.size());
        assertEquals(2, stack.pop());
    }

    @Test
    void emptyStackThrows() {
        LinkedStack<Integer> stack = new LinkedStack<>();
        assertThrows(NoSuchElementException.class, stack::pop);
        assertThrows(NoSuchElementException.class, stack::peek);
    }
}
