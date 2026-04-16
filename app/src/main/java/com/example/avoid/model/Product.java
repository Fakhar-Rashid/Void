package com.example.avoid.model;

public class Product {

    private final String name;
    private final String price;
    private final String location;
    private final String ratingSummary;

    public Product(String name, String price, String location, String ratingSummary) {
        this.name = name;
        this.price = price;
        this.location = location;
        this.ratingSummary = ratingSummary;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getLocation() {
        return location;
    }

    public String getRatingSummary() {
        return ratingSummary;
    }
}
