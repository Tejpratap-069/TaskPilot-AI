package com.taskpilot.service;

import com.taskpilot.dto.AiDtos.Suggestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FallbackParserTest {
    private final LocalFallbackParser parser = new LocalFallbackParser();
    private final LocalDate ref = LocalDate.of(2026, 10, 31); // Saturday

    @Test
    void extractsMultipleTasksWithDatesAndBoost() {
        List<Suggestion> tasks = parser.parse(
                "I have to finish my Java assignment by tomorrow, revise machine learning on Sunday, " +
                "complete my Japanese preparation before Monday, and submit my project next Friday. " +
                "Java is the most urgent.", ref);
        assertEquals(4, tasks.size(), "expected 4 extracted tasks");
        assertEquals(LocalDate.of(2026, 11, 1), tasks.get(0).dueDate());
        assertEquals("URGENT", tasks.get(0).priority(), "comparative clause should raise Java to URGENT");
        assertEquals(LocalDate.of(2026, 11, 1), tasks.get(1).dueDate());
        assertEquals(LocalDate.of(2026, 11, 2), tasks.get(2).dueDate());
        assertEquals(LocalDate.of(2026, 11, 6), tasks.get(3).dueDate());
    }

    @Test
    void extractsExplicitMonthDate() {
        List<Suggestion> tasks = parser.parse("Complete my project by November 5", ref);
        assertEquals(1, tasks.size());
        assertEquals(LocalDate.of(2026, 11, 5), tasks.get(0).dueDate());
    }

    @Test
    void missingDateProducesWarning() {
        List<Suggestion> tasks = parser.parse("Water the plants", ref);
        assertEquals(1, tasks.size());
        assertNull(tasks.get(0).dueDate());
        assertFalse(tasks.get(0).warnings().isEmpty());
    }
}
