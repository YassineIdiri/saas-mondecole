package com.example.mondecole_pocket.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username
) {
    public static AuthResponse of(String accessToken, long expiresIn, String username) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(username)
                .build();
    }
}
