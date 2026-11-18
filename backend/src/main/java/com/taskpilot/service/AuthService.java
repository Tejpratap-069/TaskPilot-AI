package com.taskpilot.service;

import com.taskpilot.dto.AuthDtos.*;
import com.taskpilot.entity.User;
import com.taskpilot.entity.UserPreference;
import com.taskpilot.exception.BadRequestException;
import com.taskpilot.exception.TooManyRequestsException;
import com.taskpilot.repository.UserPreferenceRepository;
import com.taskpilot.repository.UserRepository;
import com.taskpilot.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserRepository users, UserPreferenceRepository preferences,
                       PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwtService,
                       LoginRateLimiter loginRateLimiter) {
        this.users = users;
        this.preferences = preferences;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
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
        if (loginRateLimiter.isBlocked(email)) {
            throw new TooManyRequestsException(
                    "Too many failed login attempts. Please try again in a few minutes.");
        }
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (AuthenticationException e) {
            // Only failures count towards the limit; the exception (and its
            // generic "Invalid email or password" response) is unchanged, so
            // the response never reveals whether the email exists.
            loginRateLimiter.recordFailure(email);
            throw e;
        }
        loginRateLimiter.recordSuccess(email);
        User user = users.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found"));
        return new AuthResponse(jwtService.generateToken(email), "Bearer", toResponse(user));
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
