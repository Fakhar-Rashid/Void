package com.example.avoid.model;

public enum Condition {
    NEW("New"),
    LIKE_NEW("Like new"),
    USED("Used"),
    REFURBISHED("Refurbished");

    private final String displayName;

    Condition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
