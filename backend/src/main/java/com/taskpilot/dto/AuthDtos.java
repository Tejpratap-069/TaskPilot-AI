package com.taskpilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthResponse(String token, String tokenType, UserResponse user) {}

    public record UserResponse(Long id, String name, String email, String role) {}
}
