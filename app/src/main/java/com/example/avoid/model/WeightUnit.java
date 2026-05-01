package com.example.avoid.model;

public enum WeightUnit {
    GRAMS("g"),
    KILOGRAMS("kg"),
    OUNCES("oz"),
    POUNDS("lb");

    private final String symbol;

    WeightUnit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
