package com.taskpilot.controller;

import com.taskpilot.dto.MiscDtos.PreferenceRequest;
import com.taskpilot.dto.MiscDtos.PreferenceResponse;
import com.taskpilot.service.PreferenceService;
import com.taskpilot.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    private final PreferenceService service;
    private final SecurityUtil securityUtil;

    public PreferenceController(PreferenceService service, SecurityUtil securityUtil) {
        this.service = service;
        this.securityUtil = securityUtil;
    }

    @GetMapping
    public PreferenceResponse get() {
        return service.get(securityUtil.currentUser());
    }

    @PutMapping
    public PreferenceResponse update(@RequestBody PreferenceRequest req) {
        return service.update(securityUtil.currentUser(), req);
    }
}
