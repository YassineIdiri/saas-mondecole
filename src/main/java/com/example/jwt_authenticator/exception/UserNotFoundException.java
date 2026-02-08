package com.example.jwt_authenticator.exception;

public class UserNotFoundException extends AppException {

    private final String username;

    public UserNotFoundException(String username) {
        super("User not found", ErrorCode.USER_NOT_FOUND);
        this.username = username;
    }

    public UserNotFoundException(String message, String username) {
        super(message, ErrorCode.USER_NOT_FOUND);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
