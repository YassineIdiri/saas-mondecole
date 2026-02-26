package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "The username is required")
        @Size(min = 3, max = 50, message = "The username must contain between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "The username can only contain letters, numbers, hyphens, and underscores")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "The email cannot exceed 100 characters")
        String email,

        @NotBlank(message = "The password is required\n")
        @Size(min = 8, max = 128, message = "The password must contain between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "The password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
    public void validate() {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("The passwords do not match");
        }
    }
}