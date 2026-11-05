package com.taskpilot.dto;

import java.time.LocalDateTime;

public class MiscDtos {
    public record NotificationResponse(Long id, String type, String title, String message,
                                       Long taskId, boolean read, LocalDateTime createdAt) {}

    public record PreferenceRequest(Integer pomodoroWorkMinutes, Integer pomodoroShortBreakMinutes,
                                    Integer pomodoroLongBreakMinutes, Integer dailyGoalTasks,
                                    Boolean deadlineReminders) {}

    public record PreferenceResponse(int pomodoroWorkMinutes, int pomodoroShortBreakMinutes,
                                     int pomodoroLongBreakMinutes, int dailyGoalTasks,
                                     boolean deadlineReminders) {}
}
