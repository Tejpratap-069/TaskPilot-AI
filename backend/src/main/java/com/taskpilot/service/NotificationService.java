package com.taskpilot.service;

import com.taskpilot.dto.MiscDtos.NotificationResponse;
import com.taskpilot.entity.Notification;
import com.taskpilot.entity.Task;
import com.taskpilot.entity.User;
import com.taskpilot.enums.TaskStatus;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.repository.NotificationRepository;
import com.taskpilot.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final TaskRepository tasks;

    public NotificationService(NotificationRepository notifications, TaskRepository tasks) {
        this.notifications = notifications;
        this.tasks = tasks;
    }

    /**
     * Generates genuine deadline notifications from current task data.
     * Dedupe keys ensure the same reminder is not created twice for the same due date.
     */
    @Transactional
    public void refreshDeadlineNotifications(User user) {
        LocalDate today = LocalDate.now();
        List<Task> open = tasks.findByUserAndStatusNotInOrderByDueDateAsc(user,
                List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED));
        for (Task t : open) {
            if (t.getDueDate() == null) continue;
            String type = null, title = null, message = null, key = null;
            if (t.getDueDate().isBefore(today)) {
                type = "OVERDUE"; title = "Task overdue";
                message = "\"" + t.getTitle() + "\" was due on " + t.getDueDate() + ".";
                key = "OVERDUE-" + t.getId() + "-" + t.getDueDate();
            } else if (t.getDueDate().isEqual(today)) {
                type = "DUE_TODAY"; title = "Task due today";
                message = "\"" + t.getTitle() + "\" is due today.";
                key = "DUETODAY-" + t.getId() + "-" + t.getDueDate();
            } else if (t.getDueDate().isEqual(today.plusDays(1))) {
                type = "DUE_SOON"; title = "Task due tomorrow";
                message = "\"" + t.getTitle() + "\" is due tomorrow.";
                key = "DUESOON-" + t.getId() + "-" + t.getDueDate();
            }
            if (type != null && !notifications.existsByUserAndDedupeKey(user, key)) {
                notifications.save(Notification.builder()
                        .user(user).type(type).title(title).message(message)
                        .taskId(t.getId()).dedupeKey(key).build());
            }
        }
    }

    @Transactional
    public List<NotificationResponse> list(User user) {
        refreshDeadlineNotifications(user);
        return notifications.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByUserAndReadFalse(user);
    }

    @Transactional
    public NotificationResponse markRead(User user, Long id) {
        Notification n = notifications.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setRead(true);
        notifications.save(n);
        return toResponse(n);
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getTaskId(), n.isRead(), n.getCreatedAt());
    }
}
