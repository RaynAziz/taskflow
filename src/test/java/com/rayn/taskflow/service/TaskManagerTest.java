package com.rayn.taskflow.service;

import com.rayn.taskflow.model.Priority;
import com.rayn.taskflow.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private TaskManager tm;

    @BeforeEach
    void setUp() {
        tm = new TaskManager();
    }

    private List<Integer> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).toList();
    }

    @Test
    void nextIsHighestPriorityThenEarliestDue() {
        tm.add("low thing", Priority.LOW, TODAY);
        tm.add("high later", Priority.HIGH, TODAY.plusDays(5));
        tm.add("high sooner", Priority.HIGH, TODAY.plusDays(1));
        tm.add("high no date", Priority.HIGH, null);

        assertEquals("high sooner", tm.next().orElseThrow().getTitle());
        assertEquals(List.of(3, 2, 4, 1), ids(tm.pendingTasks()));
    }

    @Test
    void completingRemovesFromQueue() {
        Task a = tm.add("a", Priority.HIGH, null);
        tm.add("b", Priority.LOW, null);

        tm.complete(a.getId());

        assertEquals("b", tm.next().orElseThrow().getTitle());
        assertEquals(1, tm.pendingCount());
        assertEquals(2, tm.size());
        assertThrows(IllegalStateException.class, () -> tm.complete(a.getId()));
    }

    @Test
    void editingPriorityReordersQueue() {
        tm.add("first", Priority.MEDIUM, null);
        Task b = tm.add("second", Priority.LOW, null);

        tm.edit(b.getId(), null, Priority.HIGH, null, false);

        assertEquals("second", tm.next().orElseThrow().getTitle());
    }

    @Test
    void editingTitleUpdatesSearch() {
        Task t = tm.add("old name", Priority.MEDIUM, null);
        tm.edit(t.getId(), "brand new", null, null, false);

        assertTrue(tm.search("old").isEmpty());
        assertEquals(1, tm.search("bra").size());
    }

    @Test
    void invalidEditLeavesTaskUnchanged() {
        Task t = tm.add("keep me", Priority.MEDIUM, null);
        assertThrows(IllegalArgumentException.class,
                () -> tm.edit(t.getId(), "   ", null, null, false));

        assertEquals("keep me", tm.get(t.getId()).getTitle());
        assertEquals(1, tm.search("keep").size());
        assertEquals(1, tm.pendingCount());
    }

    @Test
    void undoAdd() {
        tm.add("oops", Priority.MEDIUM, null);
        assertEquals("add #1", tm.undo().orElseThrow());
        assertEquals(0, tm.size());
        assertTrue(tm.search("oops").isEmpty());
    }

    @Test
    void undoDeleteBringsTaskBack() {
        Task t = tm.add("important", Priority.HIGH, TODAY);
        tm.delete(t.getId());
        assertThrows(NoSuchElementException.class, () -> tm.get(t.getId()));

        tm.undo();

        Task back = tm.get(t.getId());
        assertEquals("important", back.getTitle());
        assertEquals(Priority.HIGH, back.getPriority());
        assertEquals(1, tm.search("imp").size());
        assertEquals(1, tm.pendingCount());
    }

    @Test
    void undoCompleteAndEditInReverseOrder() {
        Task t = tm.add("taxes", Priority.LOW, null);
        tm.edit(t.getId(), null, Priority.HIGH, TODAY, false);
        tm.complete(t.getId());

        tm.undo(); // un-complete
        assertFalse(tm.get(1).isDone());
        assertEquals(Priority.HIGH, tm.get(1).getPriority());

        tm.undo(); // un-edit
        assertEquals(Priority.LOW, tm.get(1).getPriority());
        assertNull(tm.get(1).getDueDate());

        tm.undo(); // un-add
        assertEquals(0, tm.size());
        assertTrue(tm.undo().isEmpty());
    }

    @Test
    void overdueOnlyCountsUnfinishedTasksBeforeToday() {
        tm.add("late", Priority.MEDIUM, TODAY.minusDays(2));
        Task done = tm.add("late but done", Priority.MEDIUM, TODAY.minusDays(1));
        tm.add("due today", Priority.MEDIUM, TODAY);
        tm.complete(done.getId());

        assertEquals(List.of(1), ids(tm.overdue(TODAY)));
    }

    @Test
    void loadAllContinuesIdsAfterMax() {
        Task a = new Task(4, "from disk", Priority.LOW, null);
        Task b = new Task(9, "also from disk", Priority.HIGH, null);
        tm.loadAll(List.of(a, b));

        Task c = tm.add("new", Priority.MEDIUM, null);
        assertEquals(10, c.getId());
        // loading shouldn't create undo history, only the new add should be undoable
        assertEquals("add #10", tm.undo().orElseThrow());
        assertTrue(tm.undo().isEmpty());
        assertEquals(2, tm.size());
    }

    @Test
    void loadAllRejectsDuplicateIds() {
        List<Task> dupes = List.of(new Task(1, "a", Priority.LOW, null), new Task(1, "b", Priority.LOW, null));
        assertThrows(IllegalArgumentException.class, () -> tm.loadAll(dupes));
    }
}
