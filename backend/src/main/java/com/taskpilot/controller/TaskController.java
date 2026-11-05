package com.taskpilot.controller;

import com.taskpilot.dto.TaskDtos.*;
import com.taskpilot.enums.Priority;
import com.taskpilot.enums.TaskStatus;
import com.taskpilot.service.TaskService;
import com.taskpilot.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final SecurityUtil securityUtil;

    public TaskController(TaskService taskService, SecurityUtil securityUtil) {
        this.taskService = taskService;
        this.securityUtil = securityUtil;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(securityUtil.currentUser(), req));
    }

    @GetMapping
    public Page<TaskResponse> list(@RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size,
                                   @RequestParam(defaultValue = "dueDate") String sortBy,
                                   @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy.equals("createdAt") ? "createdAt" : "dueDate");
        return taskService.list(securityUtil.currentUser(), status, priority, search,
                PageRequest.of(page, Math.min(size, 200), sort));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(securityUtil.currentUser(), id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskRequest req) {
        return taskService.update(securityUtil.currentUser(), id, req);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return taskService.updateStatus(securityUtil.currentUser(), id, req.status());
    }

    @PatchMapping("/{id}/priority")
    public TaskResponse updatePriority(@PathVariable Long id, @Valid @RequestBody PriorityUpdateRequest req) {
        return taskService.updatePriority(securityUtil.currentUser(), id, req.priority());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(securityUtil.currentUser(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<SubtaskResponse> addSubtask(@PathVariable Long id, @Valid @RequestBody SubtaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.addSubtask(securityUtil.currentUser(), id, req));
    }

    @GetMapping("/{id}/subtasks")
    public List<SubtaskResponse> listSubtasks(@PathVariable Long id) {
        return taskService.listSubtasks(securityUtil.currentUser(), id);
    }

    @PatchMapping("/{id}/subtasks/{subtaskId}/toggle")
    public SubtaskResponse toggleSubtask(@PathVariable Long id, @PathVariable Long subtaskId) {
        return taskService.toggleSubtask(securityUtil.currentUser(), id, subtaskId);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return taskService.taskHistory(securityUtil.currentUser(), id);
    }
}
