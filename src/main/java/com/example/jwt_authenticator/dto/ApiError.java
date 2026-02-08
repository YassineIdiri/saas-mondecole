package com.example.jwt_authenticator.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        Map<String, Object> details
) {
    public ApiError(Instant timestamp, int status, String error,
                    String code, String message, String path, String traceId) {
        this(timestamp, status, error, code, message, path, traceId, null);
    }

}