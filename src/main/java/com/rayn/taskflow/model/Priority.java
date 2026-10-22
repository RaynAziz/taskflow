package com.rayn.taskflow.model;

/**
 * How urgent a task is. Lower rank = more urgent, which makes it
 * easy to compare priorities inside the heap.
 */
public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /** Accepts "high", "h", "HIGH", etc. */
    public static Priority parse(String text) {
        String t = text.trim().toUpperCase();
        for (Priority p : values()) {
            if (p.name().equals(t) || p.name().startsWith(t) && !t.isEmpty()) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown priority: " + text + " (use high, medium, or low)");
    }
}
