package com.example.mondecole_pocket.entity.enums;

public enum UserRole {
    ORG_ADMIN("Administrator"),
    TEACHER("Teacher"),
    STUDENT("Student"),
    STAFF("Staff"),
    ADMIN("Admin"),
    USER("User");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}