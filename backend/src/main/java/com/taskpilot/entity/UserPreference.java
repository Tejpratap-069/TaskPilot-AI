package com.taskpilot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "pomodoro_work_minutes", nullable = false)
    @Builder.Default
    private int pomodoroWorkMinutes = 25;

    @Column(name = "pomodoro_short_break_minutes", nullable = false)
    @Builder.Default
    private int pomodoroShortBreakMinutes = 5;

    @Column(name = "pomodoro_long_break_minutes", nullable = false)
    @Builder.Default
    private int pomodoroLongBreakMinutes = 15;

    @Column(name = "daily_goal_tasks", nullable = false)
    @Builder.Default
    private int dailyGoalTasks = 5;

    @Column(nullable = false)
    @Builder.Default
    private boolean deadlineReminders = true;
}
