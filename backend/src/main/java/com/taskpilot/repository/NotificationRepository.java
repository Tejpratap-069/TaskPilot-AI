package com.taskpilot.repository;

import com.taskpilot.entity.Notification;
import com.taskpilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    Optional<Notification> findByIdAndUser(Long id, User user);
    boolean existsByUserAndDedupeKey(User user, String dedupeKey);
    long countByUserAndReadFalse(User user);
}
