package com.taskpilot.service;

import com.taskpilot.entity.Task;
import com.taskpilot.enums.Priority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Explainable urgency scoring shared by the AI prioritizer fallback and the
 * dashboard "Today's focus" recommendation. Same rules as the Python service.
 */
public final class ScoringUtil {
    private ScoringUtil() {}

    public record Score(int value, List<String> reasons) {}

    public static int weight(Priority p) {
        return switch (p) {
            case URGENT -> 40;
            case HIGH -> 30;
            case MEDIUM -> 20;
            case LOW -> 10;
        };
    }

    public static Score score(Task task, LocalDate today) {
        int value = weight(task.getPriority());
        List<String> reasons = new ArrayList<>();
        reasons.add("Priority " + task.getPriority() + " (+" + value + ")");
        LocalDate due = task.getDueDate();
        if (due == null) {
            reasons.add("No due date (+0)");
        } else {
            long days = due.toEpochDay() - today.toEpochDay();
            if (days < 0) { value += 50; reasons.add("Overdue by " + (-days) + " day(s) (+50)"); }
            else if (days == 0) { value += 45; reasons.add("Due today (+45)"); }
            else if (days == 1) { value += 35; reasons.add("Due tomorrow (+35)"); }
            else if (days <= 3) { value += 25; reasons.add("Due in " + days + " days (+25)"); }
            else if (days <= 7) { value += 15; reasons.add("Due in " + days + " days (+15)"); }
            else { value += 5; reasons.add("Due in " + days + " days (+5)"); }
        }
        return new Score(value, reasons);
    }
}
