package com.taskpilot.controller;

import com.taskpilot.dto.TaskDtos.HistoryResponse;
import com.taskpilot.service.TaskService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {
    private final TaskService taskService;
    private final SecurityUtil securityUtil;

    public ActivityController(TaskService taskService, SecurityUtil securityUtil) {
        this.taskService = taskService;
        this.securityUtil = securityUtil;
    }

    @GetMapping
    public List<HistoryResponse> activity(@RequestParam(defaultValue = "50") int limit) {
        return taskService.recentActivity(securityUtil.currentUser(), Math.min(limit, 200));
    }
}
