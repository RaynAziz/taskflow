package com.rayn.taskflow.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * A single to-do item.
 */
public class Task {

    /**
     * The order tasks come out of the queue: priority first, then the
     * earliest due date (tasks with no due date go last), then the id so
     * the ordering is always consistent.
     */
    public static final Comparator<Task> URGENCY_ORDER =
            Comparator.comparingInt((Task t) -> t.getPriority().rank())
                    .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(Task::getId);

    private final int id;
    private String title;
    private Priority priority;
    private LocalDate dueDate; // can be null
    private boolean done;

    public Task(int id, String title, Priority priority, LocalDate dueDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title can't be empty");
        }
        this.id = id;
        this.title = title.trim();
        this.priority = Objects.requireNonNull(priority);
        this.dueDate = dueDate;
        this.done = false;
    }

    /** Copy constructor, used so undo can restore the old version of a task. */
    public Task(Task other) {
        this.id = other.id;
        this.title = other.title;
        this.priority = other.priority;
        this.dueDate = other.dueDate;
        this.done = other.done;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public Priority getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isDone() { return done; }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title can't be empty");
        }
        this.title = title.trim();
    }

    public void setPriority(Priority priority) { this.priority = Objects.requireNonNull(priority); }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setDone(boolean done) { this.done = done; }

    public boolean isOverdue(LocalDate today) {
        return !done && dueDate != null && dueDate.isBefore(today);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        return id == ((Task) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        String due = dueDate == null ? "no due date" : "due " + dueDate;
        String check = done ? "[x]" : "[ ]";
        return String.format("%s #%d %-30s %-6s %s", check, id, title, priority, due);
    }
}
