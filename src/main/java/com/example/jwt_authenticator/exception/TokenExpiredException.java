package com.example.jwt_authenticator.exception;

import java.time.LocalDateTime;

public class TokenExpiredException extends AppException {

    private final LocalDateTime expiredAt;

    public TokenExpiredException(String message) {
        super(message, ErrorCode.TOKEN_EXPIRED);
        this.expiredAt = LocalDateTime.now();
    }

    public TokenExpiredException(String message, LocalDateTime expiredAt) {
        super(message, ErrorCode.TOKEN_EXPIRED);
        this.expiredAt = expiredAt != null ? expiredAt : LocalDateTime.now();
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
}
