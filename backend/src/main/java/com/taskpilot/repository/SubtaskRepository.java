package com.taskpilot.repository;

import com.taskpilot.entity.Subtask;
import com.taskpilot.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskOrderByCreatedAtAsc(Task task);
    void deleteByTask(Task task);
}
