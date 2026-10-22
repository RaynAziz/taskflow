package com.rayn.taskflow.cli;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Turns what the user types into a date. Accepts:
 *   2026-11-03   (ISO format)
 *   today, tomorrow
 *   +3           (three days from today)
 */
public final class DateInput {

    private DateInput() { }

    public static LocalDate parse(String text, LocalDate today) {
        String t = text.trim().toLowerCase();
        switch (t) {
            case "today":
                return today;
            case "tomorrow":
                return today.plusDays(1);
            default:
                break;
        }
        if (t.startsWith("+")) {
            try {
                return today.plusDays(Integer.parseInt(t.substring(1)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + text + "' isn't a valid number of days");
            }
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Couldn't read date '" + text + "'. Use YYYY-MM-DD, today, tomorrow, or +N");
        }
    }
}
