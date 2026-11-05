package com.taskpilot.dto;

import com.taskpilot.dto.TaskDtos.TaskResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardDtos {
    public record Summary(
            long totalTasks, long completedTasks, long pendingTasks, long inProgressTasks,
            long overdueTasks, long dueTodayTasks, long highPriorityTasks,
            double completionRate, long completedThisWeek) {}

    public record WeeklyStat(LocalDate date, String dayLabel, long completed, long created) {}

    public record FocusRecommendation(TaskResponse task, int score, List<String> reasons) {}

    public record PriorityCount(Map<String, Long> byPriority, Map<String, Long> byStatus) {}
}
