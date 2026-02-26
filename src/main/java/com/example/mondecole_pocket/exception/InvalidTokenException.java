package com.example.mondecole_pocket.exception;

public class InvalidTokenException extends AppException {

    public InvalidTokenException(String message) {
        super(message, ErrorCode.INVALID_TOKEN);
    }

    public InvalidTokenException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_TOKEN, cause);
    }
}
