package com.example.mondecole_pocket.entity.enums;

public enum OrganizationType {
    TRAINING_CENTER("Training center"),
    LANGUAGE_SCHOOL("Language school"),
    PRIVATE_ACADEMY("Private academy"),
    UNIVERSITY("University"),
    OTHER("Other");

    private final String displayName;

    OrganizationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}