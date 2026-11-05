package com.taskpilot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React single-page application when the frontend build is
 * bundled into the backend jar (single-service deployment).
 * Every client-side route forwards to index.html; the React router
 * takes over from there. API routes (/api/**) are handled by the
 * REST controllers and never reach this controller.
 */
@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/app",
            "/app/tasks",
            "/app/capture",
            "/app/my-day",
            "/app/calendar",
            "/app/board",
            "/app/activity",
            "/app/notifications",
            "/app/pomodoro",
            "/app/settings"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
