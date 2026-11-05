package com.taskpilot.controller;

import com.taskpilot.dto.AiDtos.*;
import com.taskpilot.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** Returns editable suggestions only. Nothing is saved until the user approves tasks via POST /api/tasks. */
    @PostMapping("/parse-tasks")
    public ParseResponse parse(@Valid @RequestBody ParseRequest req) {
        return aiService.parse(req.text());
    }

    @PostMapping("/prioritize")
    public PrioritizeResponse prioritize(@RequestBody PrioritizeRequest req) {
        return aiService.prioritize(req.tasks() != null ? req.tasks() : java.util.List.of());
    }
}
