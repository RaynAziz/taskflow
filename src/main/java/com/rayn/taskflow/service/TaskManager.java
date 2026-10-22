package com.rayn.taskflow.service;

import com.rayn.taskflow.ds.LinkedStack;
import com.rayn.taskflow.ds.MinHeap;
import com.rayn.taskflow.ds.Trie;
import com.rayn.taskflow.model.Priority;
import com.rayn.taskflow.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Keeps three structures in sync:
 *   - a HashMap from id to task, for O(1) lookup
 *   - a MinHeap of unfinished tasks, so "what's next?" is O(1)
 *   - a Trie over task titles, for prefix search
 *
 * Every change goes through insert() and detach() so the three never
 * drift apart. Undo works by saving a copy of the task before each change.
 */
public class TaskManager {

    private static final int UNDO_LIMIT = 50;

    /** One entry in the undo history. before == null means the task didn't exist yet. */
    private record Change(int taskId, Task before, String label) { }

    private final Map<Integer, Task> tasks = new HashMap<>();
    private final MinHeap<Task> pending = new MinHeap<>(Task.URGENCY_ORDER);
    private final Trie titleIndex = new Trie();
    private final LinkedStack<Change> history = new LinkedStack<>(UNDO_LIMIT);
    private int nextId = 1;

    // ---------- changes (all undoable) ----------

    public Task add(String title, Priority priority, LocalDate due) {
        Task task = new Task(nextId++, title, priority, due);
        insert(task);
        history.push(new Change(task.getId(), null, "add #" + task.getId()));
        return task;
    }

    public Task complete(int id) {
        Task task = get(id);
        if (task.isDone()) {
            throw new IllegalStateException("Task #" + id + " is already done");
        }
        return modify(task, "complete #" + id, t -> t.setDone(true));
    }

    public Task reopen(int id) {
        Task task = get(id);
        if (!task.isDone()) {
            throw new IllegalStateException("Task #" + id + " isn't done yet");
        }
        return modify(task, "reopen #" + id, t -> t.setDone(false));
    }

    /**
     * Updates whichever fields aren't null. To clear a due date, pass clearDue = true.
     */
    public Task edit(int id, String newTitle, Priority newPriority, LocalDate newDue, boolean clearDue) {
        Task task = get(id);
        return modify(task, "edit #" + id, t -> {
            if (newTitle != null) t.setTitle(newTitle);
            if (newPriority != null) t.setPriority(newPriority);
            if (clearDue) t.setDueDate(null);
            else if (newDue != null) t.setDueDate(newDue);
        });
    }

    public Task delete(int id) {
        Task task = get(id);
        Task before = new Task(task);
        detach(task);
        history.push(new Change(id, before, "delete #" + id));
        return task;
    }

    /**
     * Reverts the most recent change. Returns a short description of what
     * was undone, or empty if there's nothing to undo.
     */
    public Optional<String> undo() {
        if (history.isEmpty()) return Optional.empty();
        Change change = history.pop();
        Task current = tasks.get(change.taskId());
        if (current != null) detach(current);
        if (change.before() != null) insert(new Task(change.before()));
        return Optional.of(change.label());
    }

    // ---------- queries (read-only) ----------

    public Task get(int id) {
        Task task = tasks.get(id);
        if (task == null) throw new NoSuchElementException("No task with id #" + id);
        return task;
    }

    /** The most urgent unfinished task, if there is one. */
    public Optional<Task> next() {
        return pending.isEmpty() ? Optional.empty() : Optional.of(pending.peek());
    }

    /** Unfinished tasks, most urgent first. */
    public List<Task> pendingTasks() {
        return pending.toSortedList();
    }

    public List<Task> doneTasks() {
        List<Task> done = new ArrayList<>();
        for (Task t : tasks.values()) {
            if (t.isDone()) done.add(t);
        }
        done.sort(Comparator.comparingInt(Task::getId));
        return done;
    }

    public List<Task> allTasks() {
        List<Task> all = new ArrayList<>(pendingTasks());
        all.addAll(doneTasks());
        return all;
    }

    public List<Task> overdue(LocalDate today) {
        List<Task> result = new ArrayList<>();
        for (Task t : pendingTasks()) {
            if (t.isOverdue(today)) result.add(t);
        }
        return result;
    }

    /** Tasks with any word in the title starting with prefix, most urgent first. */
    public List<Task> search(String prefix) {
        List<Task> result = new ArrayList<>();
        for (int id : titleIndex.search(prefix)) {
            result.add(tasks.get(id));
        }
        result.sort(Comparator.comparing(Task::isDone).thenComparing(Task.URGENCY_ORDER));
        return result;
    }

    public int size() { return tasks.size(); }
    public int pendingCount() { return pending.size(); }
    public boolean canUndo() { return !history.isEmpty(); }

    // ---------- loading saved data ----------

    /** Replaces everything with tasks loaded from disk. Clears undo history. */
    public void loadAll(Collection<Task> loaded) {
        tasks.clear();
        pending.clear();
        titleIndex.clear();
        history.clear();
        int maxId = 0;
        for (Task t : loaded) {
            if (tasks.containsKey(t.getId())) {
                throw new IllegalArgumentException("Duplicate task id #" + t.getId() + " in saved data");
            }
            insert(t);
            maxId = Math.max(maxId, t.getId());
        }
        nextId = maxId + 1;
    }

    // ---------- internals ----------

    private interface Mutation {
        void apply(Task t);
    }

    /**
     * Pulls the task out of every structure, changes it, then puts it back.
     * This matters for the heap: if a task's priority changes while it's
     * still inside, the heap order breaks.
     */
    private Task modify(Task task, String label, Mutation mutation) {
        Task before = new Task(task);
        detach(task);
        try {
            mutation.apply(task);
        } catch (RuntimeException e) {
            insert(before); // put the original back if the edit was invalid
            throw e;
        }
        insert(task);
        history.push(new Change(task.getId(), before, label));
        return task;
    }

    private void insert(Task t) {
        tasks.put(t.getId(), t);
        titleIndex.insert(t.getTitle(), t.getId());
        if (!t.isDone()) pending.add(t);
    }

    private void detach(Task t) {
        tasks.remove(t.getId());
        titleIndex.remove(t.getTitle(), t.getId());
        pending.remove(t);
    }
}
