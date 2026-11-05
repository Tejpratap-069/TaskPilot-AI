package com.taskpilot.entity;

import com.taskpilot.enums.HistoryAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * History rows deliberately store task_id as a plain column (declared in
 * schema.sql as a nullable FK with ON DELETE SET NULL) so the activity
 * timeline keeps a record even after the task itself is deleted.
 */
@Entity
@Table(name = "task_history", indexes = {
        @Index(name = "idx_history_task", columnList = "task_id,created_at"),
        @Index(name = "idx_history_user", columnList = "user_id,created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_title", length = 240)
    private String taskTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HistoryAction action;

    @Column(name = "field_name", length = 60)
    private String fieldName;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(length = 500)
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
