package com.taskpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AiDtos {
    public record ParseRequest(@NotBlank @Size(max = 5000) String text) {}

    public record Suggestion(
            String title, String description, LocalDate dueDate, LocalTime dueTime,
            String priority, String category, Integer estimatedMinutes, String dependsOn,
            double confidence, List<String> warnings) {}

    public record ParseResponse(List<Suggestion> tasks, String engine, boolean fallbackUsed) {}

    public record PrioritizeItem(String title, LocalDate dueDate, String priority) {}

    public record PrioritizeRequest(List<PrioritizeItem> tasks) {}

    public record ScoredItem(String title, LocalDate dueDate, String priority, int score, List<String> reasons) {}

    public record PrioritizeResponse(List<ScoredItem> tasks, String engine) {}
}
