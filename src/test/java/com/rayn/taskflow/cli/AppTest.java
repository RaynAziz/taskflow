package com.rayn.taskflow.cli;

import com.rayn.taskflow.service.TaskManager;
import com.rayn.taskflow.storage.CsvTaskStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the whole app with scripted input, the same way a user would. */
class AppTest {

    @TempDir
    Path dir;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);

    private String run(Path file, String... lines) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));
        new App(new TaskManager(), new CsvTaskStore(file), in, out, clock).run();
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void addListAndNext() throws IOException {
        String out = run(dir.resolve("t.csv"),
                "add Water the plants -p low -d +3",
                "add \"Renew car registration\" -p high -d tomorrow",
                "next",
                "quit");

        assertTrue(out.contains("Added #1: Water the plants"));
        assertTrue(out.contains("Up next:"));
        assertTrue(out.contains("Renew car registration"));
        assertTrue(out.contains("2026-10-22"));
    }

    @Test
    void tasksSurviveRestart() throws IOException {
        Path file = dir.resolve("t.csv");
        run(file, "add Call the bank -p high", "quit");

        String out = run(file, "list", "quit");
        assertTrue(out.contains("1 task(s) to do"));
        assertTrue(out.contains("Call the bank"));
    }

    @Test
    void badInputShowsErrorAndKeepsGoing() throws IOException {
        String out = run(dir.resolve("t.csv"),
                "done 99",
                "add Something -p urgent",
                "add Something -d someday",
                "frobnicate",
                "add Still works",
                "quit");

        assertTrue(out.contains("No task with id #99"));
        assertTrue(out.contains("Unknown priority"));
        assertTrue(out.contains("Couldn't read date"));
        assertTrue(out.contains("Unknown command"));
        assertTrue(out.contains("Added #1: Still works"));
    }

    @Test
    void overdueWarningOnStartup() throws IOException {
        Path file = dir.resolve("t.csv");
        run(file, "add Return library book -d 2026-10-01", "quit");

        String out = run(file, "quit");
        assertTrue(out.contains("1 task(s) overdue"));
    }

    @Test
    void deleteThenUndo() throws IOException {
        String out = run(dir.resolve("t.csv"),
                "add Pay rent",
                "delete 1",
                "undo",
                "list",
                "quit");

        assertTrue(out.contains("Undid: delete #1"));
        assertTrue(out.trim().lines().anyMatch(l -> l.contains("Pay rent") && l.contains("MEDIUM")));
    }
}
