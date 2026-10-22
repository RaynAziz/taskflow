package com.rayn.taskflow.storage;

import com.rayn.taskflow.model.Priority;
import com.rayn.taskflow.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Saves tasks to a CSV file and reads them back.
 *
 * Format: id,title,priority,due,done
 * Titles are always quoted, and quotes inside a title are doubled
 * ("" -> "), which is the standard CSV rule, so titles can contain commas.
 *
 * Saving writes to a temp file first and then renames it over the real
 * one, so a crash halfway through a save can't wipe out your tasks.
 */
public class CsvTaskStore {

    static final String HEADER = "id,title,priority,due,done";

    private final Path file;

    public CsvTaskStore(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    public void save(Collection<Task> tasks) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Task t : tasks) {
            lines.add(toLine(t));
        }
        Path dir = file.toAbsolutePath().getParent();
        if (dir != null) Files.createDirectories(dir);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Returns an empty list if the file doesn't exist yet (first run). */
    public List<Task> load() throws IOException {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(file)) return tasks;

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || (i == 0 && line.equals(HEADER))) continue;
            try {
                tasks.add(fromLine(line));
            } catch (RuntimeException e) {
                throw new IOException("Couldn't read line " + (i + 1) + " of " + file + ": " + e.getMessage(), e);
            }
        }
        return tasks;
    }

    static String toLine(Task t) {
        String due = t.getDueDate() == null ? "" : t.getDueDate().toString();
        return t.getId() + "," + quote(t.getTitle()) + "," + t.getPriority() + "," + due + "," + t.isDone();
    }

    static Task fromLine(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() != 5) {
            throw new IllegalArgumentException("expected 5 fields but found " + fields.size());
        }
        int id = Integer.parseInt(fields.get(0).trim());
        String title = fields.get(1);
        Priority priority = Priority.valueOf(fields.get(2).trim());
        LocalDate due;
        try {
            due = fields.get(3).isBlank() ? null : LocalDate.parse(fields.get(3).trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("bad date '" + fields.get(3) + "'");
        }
        Task t = new Task(id, title, priority, due);
        t.setDone(Boolean.parseBoolean(fields.get(4).trim()));
        return t;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /** Splits one CSV line, handling quoted fields with commas and "" inside. */
    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (inQuotes) throw new IllegalArgumentException("unclosed quote");
        fields.add(cur.toString());
        return fields;
    }
}
