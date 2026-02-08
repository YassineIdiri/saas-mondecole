package com.example.jwt_authenticator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {
    private String errorCode;
    private String message;
    private String details;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private String path;

    public ErrorResponse(String errorCode, String message, String details, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    public ErrorResponse(String errorCode, String message, String path) {
        this(errorCode, message, null, path);
    }

    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, null, null);
    }
}
