package com.rayn.taskflow.storage;

import com.rayn.taskflow.model.Priority;
import com.rayn.taskflow.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTaskStoreTest {

    @TempDir
    Path dir;

    @Test
    void saveThenLoadGivesSameTasks() throws IOException {
        Task a = new Task(1, "Buy milk, eggs, \"good\" bread", Priority.LOW, LocalDate.of(2026, 11, 2));
        Task b = new Task(2, "Submit tax return", Priority.HIGH, null);
        b.setDone(true);

        CsvTaskStore store = new CsvTaskStore(dir.resolve("sub/tasks.csv"));
        store.save(List.of(a, b));
        List<Task> loaded = store.load();

        assertEquals(2, loaded.size());
        Task la = loaded.get(0);
        assertEquals("Buy milk, eggs, \"good\" bread", la.getTitle());
        assertEquals(Priority.LOW, la.getPriority());
        assertEquals(LocalDate.of(2026, 11, 2), la.getDueDate());
        assertFalse(la.isDone());

        Task lb = loaded.get(1);
        assertNull(lb.getDueDate());
        assertTrue(lb.isDone());
    }

    @Test
    void missingFileMeansNoTasks() throws IOException {
        assertTrue(new CsvTaskStore(dir.resolve("nope.csv")).load().isEmpty());
    }

    @Test
    void badLineGivesHelpfulError() throws IOException {
        Path file = dir.resolve("broken.csv");
        Files.writeString(file, CsvTaskStore.HEADER + "\n1,\"ok\",MEDIUM,,false\n2,\"bad\",URGENT,,false\n");

        IOException e = assertThrows(IOException.class, () -> new CsvTaskStore(file).load());
        assertTrue(e.getMessage().contains("line 3"), e.getMessage());
    }

    @Test
    void splitsQuotedFields() {
        assertEquals(List.of("1", "a, b", "x"), CsvTaskStore.splitCsv("1,\"a, b\",x"));
        assertEquals(List.of("say \"hi\""), CsvTaskStore.splitCsv("\"say \"\"hi\"\"\""));
    }
}
