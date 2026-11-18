package com.taskpilot.repository;

import com.taskpilot.entity.Attachment;
import com.taskpilot.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskOrderByCreatedAtAsc(Task task);
    void deleteByTaskId(Long taskId);
}
