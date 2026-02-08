package com.example.jwt_authenticator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "The username is required")
        @Size(min = 3, max = 100, message = "The username must contain between 3 and 100 characters")
        String username,

        @NotBlank(message = "The password is required")
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "The password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        Boolean rememberMe
) {
    public boolean isRememberMe() {
        return rememberMe != null && rememberMe;
    }
}