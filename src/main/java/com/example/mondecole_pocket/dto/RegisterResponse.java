package com.example.mondecole_pocket.dto;

import java.time.LocalDateTime;

public record RegisterResponse(
        String username,
        String email,
        LocalDateTime createdAt,
        String message
) {
    public static RegisterResponse of(String username, String email, LocalDateTime createdAt) {
        return new RegisterResponse(
                username,
                email,
                createdAt,
                "Account successfully created"
        );
    }
}