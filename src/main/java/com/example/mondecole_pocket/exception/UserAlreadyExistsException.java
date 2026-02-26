package com.example.mondecole_pocket.exception;

public class UserAlreadyExistsException extends AppException {
    public UserAlreadyExistsException(String message) {
        super(message, ErrorCode.USER_ALREADY_EXIST);
    }
}