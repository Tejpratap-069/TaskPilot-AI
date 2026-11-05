package com.taskpilot.controller;

import com.taskpilot.dto.DashboardDtos.*;
import com.taskpilot.dto.TaskDtos.HistoryResponse;
import com.taskpilot.dto.TaskDtos.TaskResponse;
import com.taskpilot.service.DashboardService;
import com.taskpilot.service.TaskService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    private final TaskService taskService;
    private final SecurityUtil securityUtil;

    public DashboardController(DashboardService dashboard, TaskService taskService, SecurityUtil securityUtil) {
        this.dashboard = dashboard;
        this.taskService = taskService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/summary")
    public Summary summary() {
        return dashboard.summary(securityUtil.currentUser());
    }

    @GetMapping("/weekly")
    public List<WeeklyStat> weekly() {
        return dashboard.weekly(securityUtil.currentUser());
    }

    @GetMapping("/upcoming")
    public List<TaskResponse> upcoming(@RequestParam(defaultValue = "7") int days) {
        return dashboard.upcoming(securityUtil.currentUser(), days);
    }

    @GetMapping("/breakdown")
    public PriorityCount breakdown() {
        return dashboard.breakdown(securityUtil.currentUser());
    }

    @GetMapping("/focus")
    public FocusRecommendation focus() {
        return dashboard.focus(securityUtil.currentUser());
    }

    @GetMapping("/recent-activity")
    public List<HistoryResponse> recentActivity(@RequestParam(defaultValue = "10") int limit) {
        return taskService.recentActivity(securityUtil.currentUser(), limit);
    }
}
