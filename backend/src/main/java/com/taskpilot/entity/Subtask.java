package com.taskpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subtasks", indexes = @Index(name = "idx_subtasks_task", columnList = "task_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subtask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
