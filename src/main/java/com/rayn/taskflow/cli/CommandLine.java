package com.rayn.taskflow.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Breaks one line of input into a command name, plain words, and -flags.
 *
 *   add "Renew car registration" -p high -d tomorrow
 *   -> name = "add", words = ["Renew car registration"], flags = {p=high, d=tomorrow}
 *
 * Quotes are optional: words that aren't attached to a flag get joined back
 * together, so  add Renew car registration -p high  works too.
 */
public class CommandLine {

    private final String name;
    private final List<String> words;
    private final Map<String, String> flags;

    private CommandLine(String name, List<String> words, Map<String, String> flags) {
        this.name = name;
        this.words = words;
        this.flags = flags;
    }

    public static CommandLine parse(String input) {
        List<String> tokens = tokenize(input);
        if (tokens.isEmpty()) {
            return new CommandLine("", List.of(), Map.of());
        }
        String name = tokens.get(0).toLowerCase();
        List<String> words = new ArrayList<>();
        Map<String, String> flags = new HashMap<>();

        for (int i = 1; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            if (isFlag(tok)) {
                String key = tok.substring(1).toLowerCase();
                StringBuilder value = new StringBuilder();
                // a flag's value is everything up to the next flag
                while (i + 1 < tokens.size() && !isFlag(tokens.get(i + 1))) {
                    if (value.length() > 0) value.append(' ');
                    value.append(tokens.get(++i));
                }
                if (value.length() == 0) {
                    throw new IllegalArgumentException("Flag " + tok + " needs a value");
                }
                flags.put(key, value.toString());
            } else {
                words.add(tok);
            }
        }
        return new CommandLine(name, words, flags);
    }

    public String name() { return name; }
    public List<String> words() { return words; }
    public String text() { return String.join(" ", words); }
    public boolean has(String flag) { return flags.containsKey(flag); }
    public String flag(String flag) { return flags.get(flag); }

    /** Returns the first word as an id, with a friendly error if it's missing or not a number. */
    public int id() {
        if (words.isEmpty()) throw new IllegalArgumentException("Which task? Add an id, like: " + name + " 3");
        String w = words.get(0).replace("#", "");
        try {
            return Integer.parseInt(w);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + words.get(0) + "' isn't a task id");
        }
    }

    // a flag is "-" followed by a letter, so "+3" and "-5" aren't flags
    private static boolean isFlag(String tok) {
        return tok.length() >= 2 && tok.charAt(0) == '-' && Character.isLetter(tok.charAt(1));
    }

    static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean quotedToken = false;
        for (char c : input.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                quotedToken = true;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (cur.length() > 0 || quotedToken) tokens.add(cur.toString());
                cur.setLength(0);
                quotedToken = false;
            } else {
                cur.append(c);
            }
        }
        if (inQuotes) throw new IllegalArgumentException("Missing a closing quote");
        if (cur.length() > 0 || quotedToken) tokens.add(cur.toString());
        return tokens;
    }
}
