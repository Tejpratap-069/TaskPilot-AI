package com.taskpilot.controller;

import com.taskpilot.dto.MiscDtos.NotificationResponse;
import com.taskpilot.service.NotificationService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final SecurityUtil securityUtil;

    public NotificationController(NotificationService service, SecurityUtil securityUtil) {
        this.service = service;
        this.securityUtil = securityUtil;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return service.list(securityUtil.currentUser());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", service.unreadCount(securityUtil.currentUser()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return service.markRead(securityUtil.currentUser(), id);
    }
}
