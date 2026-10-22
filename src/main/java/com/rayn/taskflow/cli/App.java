package com.rayn.taskflow.cli;

import com.rayn.taskflow.model.Priority;
import com.rayn.taskflow.model.Task;
import com.rayn.taskflow.service.TaskManager;
import com.rayn.taskflow.storage.CsvTaskStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The interactive prompt. Reads a command, runs it, prints the result,
 * and saves to disk after anything that changes the task list.
 *
 * Input, output and the clock are passed in so tests can drive the
 * whole app without a real keyboard or a real date.
 */
public class App {

    private static final String HELP = String.join("\n",
            "Commands:",
            "  add <title> [-p high|medium|low] [-d date]   add a task (default priority: medium)",
            "  list [all|done|overdue]                      show unfinished tasks, most urgent first",
            "  next                                         show the single most urgent task",
            "  done <id>                                    mark a task as done",
            "  reopen <id>                                  mark a done task as not done",
            "  edit <id> [-t title] [-p priority] [-d date|none]",
            "  delete <id>                                  remove a task",
            "  search <text>                                find tasks by the start of any word",
            "  undo                                         undo the last change",
            "  stats                                        quick summary",
            "  help, quit",
            "",
            "Dates: 2026-11-03, today, tomorrow, or +N (N days from today)");

    private final TaskManager manager;
    private final CsvTaskStore store;
    private final BufferedReader in;
    private final PrintStream out;
    private final Clock clock;

    public App(TaskManager manager, CsvTaskStore store, BufferedReader in, PrintStream out, Clock clock) {
        this.manager = manager;
        this.store = store;
        this.in = in;
        this.out = out;
        this.clock = clock;
    }

    public void run() throws IOException {
        manager.loadAll(store.load());
        out.println("TaskFlow - " + manager.pendingCount() + " task(s) to do. Type 'help' for commands.");
        showOverdueWarning();

        while (true) {
            out.print("> ");
            out.flush();
            String line = in.readLine();
            if (line == null) break; // end of input (Ctrl+D)
            if (!handle(line)) break;
        }
        out.println("Bye!");
    }

    /** Runs one command. Returns false when the user wants to quit. */
    boolean handle(String line) {
        try {
            CommandLine cmd = CommandLine.parse(line);
            switch (cmd.name()) {
                case "":
                    return true;
                case "quit":
                case "exit":
                    return false;
                case "help":
                    out.println(HELP);
                    return true;
                case "add":
                    add(cmd);
                    break;
                case "list":
                case "ls":
                    list(cmd);
                    return true;
                case "next":
                    next();
                    return true;
                case "done":
                    out.println("Done: " + manager.complete(cmd.id()).getTitle());
                    break;
                case "reopen":
                    out.println("Reopened: " + manager.reopen(cmd.id()).getTitle());
                    break;
                case "edit":
                    edit(cmd);
                    break;
                case "delete":
                case "rm":
                    out.println("Deleted: " + manager.delete(cmd.id()).getTitle() + "  (type 'undo' to bring it back)");
                    break;
                case "search":
                case "find":
                    search(cmd);
                    return true;
                case "undo":
                    Optional<String> undone = manager.undo();
                    out.println(undone.map(s -> "Undid: " + s).orElse("Nothing to undo."));
                    if (undone.isEmpty()) return true;
                    break;
                case "stats":
                    stats();
                    return true;
                default:
                    out.println("Unknown command '" + cmd.name() + "'. Type 'help' to see what's available.");
                    return true;
            }
            save();
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException e) {
            out.println("Error: " + e.getMessage());
        }
        return true;
    }

    private void add(CommandLine cmd) {
        if (cmd.text().isBlank()) {
            throw new IllegalArgumentException("Give the task a title, like: add Pay rent -p high");
        }
        Priority p = cmd.has("p") ? Priority.parse(cmd.flag("p")) : Priority.MEDIUM;
        LocalDate due = cmd.has("d") ? DateInput.parse(cmd.flag("d"), today()) : null;
        Task t = manager.add(cmd.text(), p, due);
        out.println("Added #" + t.getId() + ": " + t.getTitle());
    }

    private void edit(CommandLine cmd) {
        int id = cmd.id();
        String title = cmd.has("t") ? cmd.flag("t") : null;
        Priority p = cmd.has("p") ? Priority.parse(cmd.flag("p")) : null;
        boolean clearDue = cmd.has("d") && cmd.flag("d").equalsIgnoreCase("none");
        LocalDate due = cmd.has("d") && !clearDue ? DateInput.parse(cmd.flag("d"), today()) : null;
        if (title == null && p == null && due == null && !clearDue) {
            throw new IllegalArgumentException("Nothing to change. Use -t, -p, or -d");
        }
        Task t = manager.edit(id, title, p, due, clearDue);
        out.println("Updated: " + format(t));
    }

    private void list(CommandLine cmd) {
        String which = cmd.words().isEmpty() ? "" : cmd.words().get(0).toLowerCase();
        List<Task> tasks;
        switch (which) {
            case "":
                tasks = manager.pendingTasks();
                break;
            case "all":
                tasks = manager.allTasks();
                break;
            case "done":
                tasks = manager.doneTasks();
                break;
            case "overdue":
                tasks = manager.overdue(today());
                break;
            default:
                throw new IllegalArgumentException("Use: list, list all, list done, or list overdue");
        }
        printTasks(tasks, "No tasks here.");
    }

    private void next() {
        Optional<Task> t = manager.next();
        if (t.isPresent()) {
            out.println("Up next: " + format(t.get()));
        } else {
            out.println("You're all caught up.");
        }
    }

    private void search(CommandLine cmd) {
        if (cmd.text().isBlank()) throw new IllegalArgumentException("Search for what? e.g. search rent");
        // search each word and keep tasks that match all of them
        List<String> terms = List.of(cmd.text().trim().split("\\s+"));
        List<Task> results = manager.search(terms.get(0));
        for (int i = 1; i < terms.size(); i++) {
            List<Task> more = manager.search(terms.get(i));
            results.retainAll(more);
        }
        printTasks(results, "No matches for '" + cmd.text() + "'.");
    }

    private void stats() {
        int total = manager.size();
        int open = manager.pendingCount();
        int finished = total - open;
        int late = manager.overdue(today()).size();
        int pct = total == 0 ? 0 : Math.round(100f * finished / total);
        out.printf("%d total | %d to do | %d done (%d%%) | %d overdue%n", total, open, finished, pct, late);
    }

    private void showOverdueWarning() {
        int late = manager.overdue(today()).size();
        if (late > 0) {
            out.println("Heads up: " + late + " task(s) overdue. Type 'list overdue' to see them.");
        }
    }

    private void printTasks(List<Task> tasks, String emptyMessage) {
        if (tasks.isEmpty()) {
            out.println(emptyMessage);
            return;
        }
        out.println("   ID  PRIORITY  DUE          TITLE");
        for (Task t : tasks) {
            out.println(format(t));
        }
    }

    private String format(Task t) {
        String mark = t.isDone() ? "x" : t.isOverdue(today()) ? "!" : " ";
        String due = t.getDueDate() == null ? "-" : t.getDueDate().toString();
        return String.format("%s %4d  %-8s  %-11s  %s", mark, t.getId(), t.getPriority(), due, t.getTitle());
    }

    private void save() {
        try {
            store.save(manager.allTasks());
        } catch (IOException e) {
            out.println("Warning: couldn't save to " + store.getFile() + " (" + e.getMessage() + ")");
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
