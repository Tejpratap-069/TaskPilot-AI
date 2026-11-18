package com.taskpilot.service;

import com.taskpilot.entity.User;
import com.taskpilot.entity.UserPreference;
import com.taskpilot.repository.UserPreferenceRepository;
import com.taskpilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Generates deadline reminder notifications in the background so users are
 * reminded even when they have not opened the app. Runs once shortly after
 * startup and then every hour. Only users whose preference
 * {@code deadlineReminders} is enabled get reminders; the actual generation
 * (and its dedupe keys) lives in {@link NotificationService}, so a reminder
 * is never created twice for the same task and due date.
 */
@Service
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final NotificationService notificationService;

    public ReminderScheduler(UserRepository users, UserPreferenceRepository preferences,
                             NotificationService notificationService) {
        this.users = users;
        this.preferences = preferences;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 3_600_000)
    public void generateDeadlineReminders() {
        int generated = 0;
        for (User user : users.findAll()) {
            try {
                boolean enabled = preferences.findByUser(user)
                        .map(UserPreference::isDeadlineReminders)
                        .orElse(true);
                if (enabled) {
                    notificationService.refreshDeadlineNotifications(user);
                    generated++;
                }
            } catch (Exception e) {
                // One user's bad data must not stop reminders for everyone else.
                log.warn("Reminder generation failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.debug("Reminder run finished; processed {} user(s) with reminders enabled", generated);
    }
}
