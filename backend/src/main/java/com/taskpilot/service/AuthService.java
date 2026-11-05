package com.taskpilot.service;

import com.taskpilot.dto.AuthDtos.*;
import com.taskpilot.entity.User;
import com.taskpilot.entity.UserPreference;
import com.taskpilot.exception.BadRequestException;
import com.taskpilot.repository.UserPreferenceRepository;
import com.taskpilot.repository.UserRepository;
import com.taskpilot.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserPreferenceRepository preferences;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, UserPreferenceRepository preferences,
                       PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwtService) {
        this.users = users;
        this.preferences = preferences;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        User user = User.builder()
                .name(req.name().trim())
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .role("USER")
                .build();
        users.save(user);
        preferences.save(UserPreference.builder().user(user).build());
        return new AuthResponse(jwtService.generateToken(email), "Bearer", toResponse(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        User user = users.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found"));
        return new AuthResponse(jwtService.generateToken(email), "Bearer", toResponse(user));
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
