package com.example.mondecole_pocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * ════════════════════════════════════════════════════════
 * ERROR RESPONSE - Réponse d'erreur standardisée
 * ════════════════════════════════════════════════════════
 *
 * Utilisé pour retourner des erreurs au format JSON
 * avec des informations détaillées
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String details
) {

    /**
     * Créer une ErrorResponse simple
     */
    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Créer une ErrorResponse avec status HTTP
     */
    public static ErrorResponse of(int status, String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Créer une ErrorResponse complète
     */
    public static ErrorResponse of(int status, String error, String code,
                                   String message, String path, String details) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .path(path)
                .details(details)
                .build();
    }

    /**
     * Créer une ErrorResponse depuis une exception
     */
    public static ErrorResponse fromException(Exception ex, String path, int status) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(ex.getClass().getSimpleName())
                .message(ex.getMessage())
                .path(path)
                .build();
    }
}