package com.rayn.taskflow;

import com.rayn.taskflow.cli.App;
import com.rayn.taskflow.service.TaskManager;
import com.rayn.taskflow.storage.CsvTaskStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

public class Main {

    public static void main(String[] args) {
        // tasks are saved to ~/.taskflow/tasks.csv unless a different file is given
        Path file = args.length > 0
                ? Path.of(args[0])
                : Path.of(System.getProperty("user.home"), ".taskflow", "tasks.csv");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        App app = new App(new TaskManager(), new CsvTaskStore(file), in, System.out, Clock.systemDefaultZone());
        try {
            app.run();
        } catch (IOException e) {
            System.err.println("Couldn't open " + file + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
