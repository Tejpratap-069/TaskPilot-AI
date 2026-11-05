package com.taskpilot.service;

import com.taskpilot.dto.TaskDtos.*;
import com.taskpilot.entity.*;
import com.taskpilot.enums.HistoryAction;
import com.taskpilot.enums.TaskStatus;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final CategoryRepository categories;
    private final SubtaskRepository subtasks;
    private final TaskHistoryRepository history;
    private final NotificationRepository notifications;

    public TaskService(TaskRepository tasks, CategoryRepository categories, SubtaskRepository subtasks,
                       TaskHistoryRepository history, NotificationRepository notifications) {
        this.tasks = tasks;
        this.categories = categories;
        this.subtasks = subtasks;
        this.history = history;
        this.notifications = notifications;
    }

    // ------------------------------------------------------------------ CRUD

    @Transactional
    public TaskResponse create(User user, TaskRequest req) {
        Task task = Task.builder()
                .user(user)
                .title(req.title().trim())
                .description(req.description())
                .status(req.status() != null ? req.status() : TaskStatus.TODO)
                .priority(req.priority() != null ? req.priority() : com.taskpilot.enums.Priority.MEDIUM)
                .dueDate(req.dueDate())
                .dueTime(req.dueTime())
                .category(resolveCategory(user, req.category()))
                .estimatedMinutes(req.estimatedMinutes())
                .dependsOn(req.dependsOn())
                .build();
        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }
        tasks.save(task);
        record(user, task, HistoryAction.CREATED, null, null, task.getTitle(),
                "Task created" + (req.category() != null ? " in category " + req.category() : ""));
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(User user, TaskStatus status, com.taskpilot.enums.Priority priority,
                                   String search, Pageable pageable) {
        String s = (search == null || search.isBlank()) ? null : search.trim();
        return tasks.searchTasks(user, status, priority, s, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(User user, Long id) {
        return toResponse(ownedTask(user, id));
    }

    @Transactional
    public TaskResponse update(User user, Long id, TaskRequest req) {
        Task task = ownedTask(user, id);
        if (req.title() != null && !req.title().isBlank() && !req.title().trim().equals(task.getTitle())) {
            record(user, task, HistoryAction.UPDATED, "title", task.getTitle(), req.title().trim(), "Title changed");
            task.setTitle(req.title().trim());
        }
        if (req.description() != null) task.setDescription(req.description());
        if (req.dueDate() != null && !req.dueDate().equals(task.getDueDate())) {
            record(user, task, HistoryAction.DEADLINE_CHANGED, "dueDate",
                    String.valueOf(task.getDueDate()), String.valueOf(req.dueDate()), "Deadline changed");
            task.setDueDate(req.dueDate());
        }
        if (req.dueTime() != null) task.setDueTime(req.dueTime());
        if (req.priority() != null && req.priority() != task.getPriority()) {
            record(user, task, HistoryAction.PRIORITY_CHANGED, "priority",
                    task.getPriority().name(), req.priority().name(), "Priority changed");
            task.setPriority(req.priority());
        }
        if (req.category() != null) task.setCategory(resolveCategory(user, req.category()));
        if (req.estimatedMinutes() != null) task.setEstimatedMinutes(req.estimatedMinutes());
        if (req.dependsOn() != null) task.setDependsOn(req.dependsOn());
        if (req.status() != null && req.status() != task.getStatus()) {
            applyStatus(user, task, req.status());
        }
        tasks.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(User user, Long id, TaskStatus status) {
        Task task = ownedTask(user, id);
        if (task.getStatus() != status) {
            applyStatus(user, task, status);
            tasks.save(task);
        }
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updatePriority(User user, Long id, com.taskpilot.enums.Priority priority) {
        Task task = ownedTask(user, id);
        if (task.getPriority() != priority) {
            record(user, task, HistoryAction.PRIORITY_CHANGED, "priority",
                    task.getPriority().name(), priority.name(), "Priority changed");
            task.setPriority(priority);
            tasks.save(task);
        }
        return toResponse(task);
    }

    @Transactional
    public void delete(User user, Long id) {
        Task task = ownedTask(user, id);
        record(user, task, HistoryAction.DELETED, null, task.getTitle(), null, "Task deleted");
        subtasks.deleteByTask(task);
        tasks.delete(task);
    }

    // -------------------------------------------------------------- subtasks

    @Transactional
    public SubtaskResponse addSubtask(User user, Long taskId, SubtaskRequest req) {
        Task task = ownedTask(user, taskId);
        Subtask sub = Subtask.builder().task(task).title(req.title().trim()).completed(false).build();
        subtasks.save(sub);
        record(user, task, HistoryAction.UPDATED, "subtask", null, sub.getTitle(), "Subtask added");
        return toSubtaskResponse(sub);
    }

    @Transactional(readOnly = true)
    public List<SubtaskResponse> listSubtasks(User user, Long taskId) {
        Task task = ownedTask(user, taskId);
        return subtasks.findByTaskOrderByCreatedAtAsc(task).stream().map(TaskService::toSubtaskResponse).toList();
    }

    @Transactional
    public SubtaskResponse toggleSubtask(User user, Long taskId, Long subtaskId) {
        Task task = ownedTask(user, taskId);
        Subtask sub = subtasks.findById(subtaskId)
                .filter(s -> s.getTask().getId().equals(task.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
        sub.setCompleted(!sub.isCompleted());
        subtasks.save(sub);
        return toSubtaskResponse(sub);
    }

    // --------------------------------------------------------------- history

    @Transactional(readOnly = true)
    public List<HistoryResponse> taskHistory(User user, Long taskId) {
        ownedTask(user, taskId); // ownership check
        return history.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(TaskService::toHistoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> recentActivity(User user, int limit) {
        return history.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(TaskService::toHistoryResponse).toList();
    }

    // --------------------------------------------------------------- helpers

    public Task ownedTask(User user, Long id) {
        return tasks.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private Category resolveCategory(User user, String name) {
        if (name == null || name.isBlank()) return null;
        String clean = name.trim();
        return categories.findByUserAndNameIgnoreCase(user, clean)
                .orElseGet(() -> categories.save(Category.builder().user(user).name(clean).build()));
    }

    private void applyStatus(User user, Task task, TaskStatus next) {
        TaskStatus prev = task.getStatus();
        if (next == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
            record(user, task, HistoryAction.COMPLETED, "status", prev.name(), next.name(), "Task completed");
            notifications.save(Notification.builder()
                    .user(user).type("COMPLETED")
                    .title("Task completed")
                    .message("\"" + task.getTitle() + "\" was marked as completed.")
                    .taskId(task.getId())
                    .dedupeKey("DONE-" + task.getId() + "-" + System.currentTimeMillis())
                    .build());
        } else if (prev == TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
            record(user, task, HistoryAction.REOPENED, "status", prev.name(), next.name(), "Task reopened");
        } else {
            record(user, task, HistoryAction.STATUS_CHANGED, "status", prev.name(), next.name(), "Status changed");
        }
        task.setStatus(next);
    }

    public void record(User user, Task task, HistoryAction action, String field,
                       String oldValue, String newValue, String detail) {
        history.save(TaskHistory.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .user(user)
                .action(action)
                .fieldName(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .detail(detail)
                .build());
    }

    public TaskResponse toResponse(Task task) {
        List<SubtaskResponse> subs = subtasks.findByTaskOrderByCreatedAtAsc(task)
                .stream().map(TaskService::toSubtaskResponse).toList();
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(),
                task.getDueDate(), task.getDueTime(),
                task.getCategory() != null ? task.getCategory().getName() : null,
                task.getEstimatedMinutes(), task.getDependsOn(), task.getCompletedAt(),
                task.getCreatedAt(), task.getUpdatedAt(), subs);
    }

    public static SubtaskResponse toSubtaskResponse(Subtask s) {
        return new SubtaskResponse(s.getId(), s.getTask().getId(), s.getTitle(), s.isCompleted(), s.getCreatedAt());
    }

    public static HistoryResponse toHistoryResponse(TaskHistory h) {
        return new HistoryResponse(h.getId(), h.getTaskId(), h.getTaskTitle(), h.getAction().name(),
                h.getFieldName(), h.getOldValue(), h.getNewValue(), h.getDetail(), h.getCreatedAt());
    }
}
