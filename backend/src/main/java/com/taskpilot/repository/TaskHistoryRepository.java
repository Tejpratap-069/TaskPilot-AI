package com.taskpilot.repository;

import com.taskpilot.entity.TaskHistory;
import com.taskpilot.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
