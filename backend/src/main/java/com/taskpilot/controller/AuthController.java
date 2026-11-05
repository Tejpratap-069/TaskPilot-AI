package com.taskpilot.controller;

import com.taskpilot.dto.AuthDtos.*;
import com.taskpilot.service.AuthService;
import com.taskpilot.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final SecurityUtil securityUtil;

    public AuthController(AuthService auth, SecurityUtil securityUtil) {
        this.auth = auth;
        this.securityUtil = securityUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return AuthService.toResponse(securityUtil.currentUser());
    }
}
