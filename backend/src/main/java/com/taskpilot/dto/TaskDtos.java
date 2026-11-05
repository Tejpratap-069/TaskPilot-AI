package com.taskpilot.dto;

import com.taskpilot.enums.Priority;
import com.taskpilot.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TaskDtos {
    public record TaskRequest(
            @NotBlank @Size(max = 240) String title,
            @Size(max = 2000) String description,
            TaskStatus status,
            Priority priority,
            LocalDate dueDate,
            LocalTime dueTime,
            String category,
            Integer estimatedMinutes,
            String dependsOn) {}

    public record StatusUpdateRequest(@jakarta.validation.constraints.NotNull TaskStatus status) {}

    public record PriorityUpdateRequest(@jakarta.validation.constraints.NotNull Priority priority) {}

    public record SubtaskRequest(@NotBlank @Size(max = 240) String title) {}

    public record SubtaskResponse(Long id, Long taskId, String title, boolean completed, LocalDateTime createdAt) {}

    public record TaskResponse(
            Long id, String title, String description, TaskStatus status, Priority priority,
            LocalDate dueDate, LocalTime dueTime, String category, Integer estimatedMinutes,
            String dependsOn, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
            List<SubtaskResponse> subtasks) {}

    public record HistoryResponse(Long id, Long taskId, String taskTitle, String action, String fieldName,
                                  String oldValue, String newValue, String detail, LocalDateTime createdAt) {}
}
