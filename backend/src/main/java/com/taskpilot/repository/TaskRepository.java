package com.taskpilot.repository;

import com.taskpilot.entity.Task;
import com.taskpilot.entity.User;
import com.taskpilot.enums.Priority;
import com.taskpilot.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUser(Long id, User user);

    Page<Task> findByUser(User user, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.user = :user AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> searchTasks(@Param("user") User user,
                           @Param("status") TaskStatus status,
                           @Param("priority") Priority priority,
                           @Param("search") String search,
                           Pageable pageable);

    List<Task> findByUserAndStatusNotInOrderByDueDateAsc(User user, List<TaskStatus> excluded);

    List<Task> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    long countByUserAndStatus(User user, TaskStatus status);

    long countByUserAndStatusInAndDueDateBefore(User user, List<TaskStatus> statuses, LocalDate date);

    long countByUserAndStatusInAndDueDate(User user, List<TaskStatus> statuses, LocalDate date);

    long countByUserAndPriorityInAndStatusIn(User user, List<Priority> priorities, List<TaskStatus> statuses);

    List<Task> findByUserAndStatusInAndDueDateBetweenOrderByDueDateAsc(
            User user, List<TaskStatus> statuses, LocalDate from, LocalDate to);

    List<Task> findByUserAndStatusInAndDueDateOrderByDueDateAsc(
            User user, List<TaskStatus> statuses, LocalDate date);

    long countByUserAndCompletedAtBetween(User user, LocalDateTime from, LocalDateTime to);

    long countByUserAndCreatedAtBetween(User user, LocalDateTime from, LocalDateTime to);

    List<Task> findByUserAndDueDateBetweenOrderByDueDateAsc(User user, LocalDate from, LocalDate to);
}
