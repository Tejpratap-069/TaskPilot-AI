package com.taskpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_user_read", columnList = "user_id,is_read"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String type; // OVERDUE, DUE_TODAY, DUE_SOON, COMPLETED, INFO

    @Column(nullable = false, length = 240)
    private String title;

    @Column(length = 500)
    private String message;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "dedupe_key", length = 190)
    private String dedupeKey;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
