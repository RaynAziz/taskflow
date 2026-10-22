package com.rayn.taskflow.cli;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineTest {

    @Test
    void parsesWordsAndFlags() {
        CommandLine cmd = CommandLine.parse("add Renew car registration -p high -d tomorrow");
        assertEquals("add", cmd.name());
        assertEquals("Renew car registration", cmd.text());
        assertEquals("high", cmd.flag("p"));
        assertEquals("tomorrow", cmd.flag("d"));
    }

    @Test
    void quotedTitleCanContainDashes() {
        CommandLine cmd = CommandLine.parse("add \"Fix bug -p in parser\" -p low");
        assertEquals("Fix bug -p in parser", cmd.text());
        assertEquals("low", cmd.flag("p"));
    }

    @Test
    void plusNIsNotAFlag() {
        CommandLine cmd = CommandLine.parse("add thing -d +3");
        assertEquals("+3", cmd.flag("d"));
    }

    @Test
    void flagWithoutValueIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("add thing -p"));
    }

    @Test
    void unclosedQuoteIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("add \"oops"));
    }

    @Test
    void idAcceptsHashPrefix() {
        assertEquals(3, CommandLine.parse("done #3").id());
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("done abc").id());
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("done").id());
    }

    @Test
    void tokenizeKeepsEmptyQuotedString() {
        assertEquals(List.of("add", ""), CommandLine.tokenize("add \"\""));
    }

    @Test
    void dateShortcuts() {
        LocalDate today = LocalDate.of(2026, 10, 21);
        assertEquals(today, DateInput.parse("today", today));
        assertEquals(today.plusDays(1), DateInput.parse("Tomorrow", today));
        assertEquals(today.plusDays(10), DateInput.parse("+10", today));
        assertEquals(LocalDate.of(2026, 12, 1), DateInput.parse("2026-12-01", today));
        assertThrows(IllegalArgumentException.class, () -> DateInput.parse("next week", today));
    }
}
