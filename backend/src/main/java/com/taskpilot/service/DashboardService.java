package com.taskpilot.service;

import com.taskpilot.dto.DashboardDtos.*;
import com.taskpilot.dto.TaskDtos.TaskResponse;
import com.taskpilot.entity.Task;
import com.taskpilot.entity.User;
import com.taskpilot.enums.Priority;
import com.taskpilot.enums.TaskStatus;
import com.taskpilot.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class DashboardService {
    private static final List<TaskStatus> ACTIVE = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);
    private static final List<TaskStatus> OPEN = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final TaskRepository tasks;
    private final TaskService taskService;

    public DashboardService(TaskRepository tasks, TaskService taskService) {
        this.tasks = tasks;
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public Summary summary(User user) {
        LocalDate today = LocalDate.now();
        long total = tasks.countByUser(user);
        long completed = tasks.countByUserAndStatus(user, TaskStatus.COMPLETED);
        long inProgress = tasks.countByUserAndStatus(user, TaskStatus.IN_PROGRESS);
        long pending = tasks.countByUserAndStatus(user, TaskStatus.TODO);
        long overdue = tasks.countByUserAndStatusInAndDueDateBefore(user, ACTIVE, today);
        long dueToday = tasks.countByUserAndStatusInAndDueDate(user, ACTIVE, today);
        long high = tasks.countByUserAndPriorityInAndStatusIn(user,
                List.of(Priority.HIGH, Priority.URGENT), ACTIVE);
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        long completedWeek = tasks.countByUserAndCompletedAtBetween(user, weekStart, LocalDateTime.now());
        double rate = total == 0 ? 0.0 : Math.round(completed * 1000.0 / total) / 10.0;
        return new Summary(total, completed, pending, inProgress, overdue, dueToday, high, rate, completedWeek);
    }

    @Transactional(readOnly = true)
    public List<WeeklyStat> weekly(User user) {
        LocalDate today = LocalDate.now();
        List<WeeklyStat> out = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to = day.atTime(LocalTime.MAX);
            long completed = tasks.countByUserAndCompletedAtBetween(user, from, to);
            long created = tasks.countByUserAndCreatedAtBetween(user, from, to);
            out.add(new WeeklyStat(day, day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), completed, created));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> upcoming(User user, int days) {
        LocalDate today = LocalDate.now();
        return tasks.findByUserAndStatusInAndDueDateBetweenOrderByDueDateAsc(user, OPEN, today, today.plusDays(days))
                .stream().map(taskService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> todayTasks(User user) {
        return tasks.findByUserAndStatusInAndDueDateOrderByDueDateAsc(user, OPEN, LocalDate.now())
                .stream().map(taskService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> overdueTasks(User user) {
        return tasks.findByUserAndStatusInAndDueDateBetweenOrderByDueDateAsc(user, OPEN,
                        LocalDate.now().minusYears(5), LocalDate.now().minusDays(1))
                .stream().map(taskService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PriorityCount breakdown(User user) {
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), tasks.countByUserAndPriorityInAndStatusIn(user, List.of(p),
                    List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED)));
        }
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), tasks.countByUserAndStatus(user, s));
        }
        return new PriorityCount(byPriority, byStatus);
    }

    /**
     * Productivity insights, all computed from the user's real task data:
     * completions per week for the last 8 weeks (Monday-start), per-category
     * open/completed counts, and the current completion streak in days
     * (consecutive days with at least one completion, counting back from
     * today — or from yesterday if today has none yet).
     */
    @Transactional(readOnly = true)
    public Insights insights(User user) {
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate earliestWeekStart = currentWeekStart.minusWeeks(7);

        // One fetch of the last ~year of completions feeds both the weekly
        // buckets and the streak.
        List<Task> completed = tasks.findByUserAndCompletedAtBetween(
                user, today.minusDays(366).atStartOfDay(), LocalDateTime.now());

        Map<LocalDate, Long> byWeek = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) byWeek.put(earliestWeekStart.plusWeeks(i), 0L);
        Set<LocalDate> completionDays = new HashSet<>();
        for (Task t : completed) {
            if (t.getCompletedAt() == null) continue;
            LocalDate day = t.getCompletedAt().toLocalDate();
            completionDays.add(day);
            LocalDate weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (byWeek.containsKey(weekStart)) byWeek.merge(weekStart, 1L, Long::sum);
        }
        List<WeekCompletion> weeks = byWeek.entrySet().stream()
                .map(e -> new WeekCompletion(e.getKey(), e.getValue()))
                .toList();

        int streak = 0;
        LocalDate cursor = completionDays.contains(today) ? today : today.minusDays(1);
        while (completionDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        Map<String, long[]> catCounts = new LinkedHashMap<>();
        for (Object[] row : tasks.countByCategoryAndStatus(user)) {
            String name = row[0] == null ? "Uncategorized" : (String) row[0];
            TaskStatus status = (TaskStatus) row[1];
            long count = (Long) row[2];
            long[] pair = catCounts.computeIfAbsent(name, k -> new long[2]);
            if (status == TaskStatus.COMPLETED) pair[1] += count;
            else if (status == TaskStatus.TODO || status == TaskStatus.IN_PROGRESS) pair[0] += count;
        }
        List<CategoryCount> byCategory = catCounts.entrySet().stream()
                .map(e -> new CategoryCount(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new Insights(weeks, byCategory, streak, summary(user));
    }

    /** "Today's focus": the highest-scoring open task by the explainable urgency score. */
    @Transactional(readOnly = true)
    public FocusRecommendation focus(User user) {
        LocalDate today = LocalDate.now();
        List<Task> open = tasks.findByUserAndStatusNotInOrderByDueDateAsc(user,
                List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED));
        Task best = null;
        ScoringUtil.Score bestScore = null;
        for (Task t : open) {
            ScoringUtil.Score s = ScoringUtil.score(t, today);
            if (bestScore == null || s.value() > bestScore.value()) {
                best = t; bestScore = s;
            }
        }
        if (best == null) return null;
        return new FocusRecommendation(taskService.toResponse(best), bestScore.value(), bestScore.reasons());
    }
}
