package com.taskpilot.service;

import com.taskpilot.dto.MiscDtos.PreferenceRequest;
import com.taskpilot.dto.MiscDtos.PreferenceResponse;
import com.taskpilot.entity.User;
import com.taskpilot.entity.UserPreference;
import com.taskpilot.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {
    private final UserPreferenceRepository prefs;

    public PreferenceService(UserPreferenceRepository prefs) {
        this.prefs = prefs;
    }

    @Transactional
    public PreferenceResponse get(User user) {
        return toResponse(getOrCreate(user));
    }

    @Transactional
    public PreferenceResponse update(User user, PreferenceRequest req) {
        UserPreference p = getOrCreate(user);
        if (req.pomodoroWorkMinutes() != null) p.setPomodoroWorkMinutes(req.pomodoroWorkMinutes());
        if (req.pomodoroShortBreakMinutes() != null) p.setPomodoroShortBreakMinutes(req.pomodoroShortBreakMinutes());
        if (req.pomodoroLongBreakMinutes() != null) p.setPomodoroLongBreakMinutes(req.pomodoroLongBreakMinutes());
        if (req.dailyGoalTasks() != null) p.setDailyGoalTasks(req.dailyGoalTasks());
        if (req.deadlineReminders() != null) p.setDeadlineReminders(req.deadlineReminders());
        prefs.save(p);
        return toResponse(p);
    }

    private UserPreference getOrCreate(User user) {
        return prefs.findByUser(user).orElseGet(() -> prefs.save(UserPreference.builder().user(user).build()));
    }

    private static PreferenceResponse toResponse(UserPreference p) {
        return new PreferenceResponse(p.getPomodoroWorkMinutes(), p.getPomodoroShortBreakMinutes(),
                p.getPomodoroLongBreakMinutes(), p.getDailyGoalTasks(), p.isDeadlineReminders());
    }
}
