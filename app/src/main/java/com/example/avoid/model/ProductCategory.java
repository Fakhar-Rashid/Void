package com.example.avoid.model;

public enum ProductCategory {
    LAPTOP("Laptop"),
    SMARTPHONE("Smartphone"),
    MONITOR("Monitor"),
    ACCESSORIES("Accessories");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
