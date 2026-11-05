package com.taskpilot.controller;

import com.taskpilot.dto.TaskDtos.TaskResponse;
import com.taskpilot.service.DashboardService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {
    private final DashboardService dashboard;
    private final SecurityUtil securityUtil;

    public PlannerController(DashboardService dashboard, SecurityUtil securityUtil) {
        this.dashboard = dashboard;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/today")
    public Map<String, List<TaskResponse>> today() {
        var user = securityUtil.currentUser();
        return Map.of(
                "today", dashboard.todayTasks(user),
                "overdue", dashboard.overdueTasks(user));
    }

    @GetMapping("/upcoming")
    public List<TaskResponse> upcoming(@RequestParam(defaultValue = "14") int days) {
        return dashboard.upcoming(securityUtil.currentUser(), days);
    }
}
