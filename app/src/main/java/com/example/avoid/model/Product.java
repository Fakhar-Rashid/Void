package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {

    public static final String CATEGORY_BEST_SELLER = "best_seller";
    public static final String CATEGORY_RECOMMENDATION = "recommendation";

    private String id;
    private String name;
    private String price;
    private String location;
    private String ratingSummary;
    private String category;
    private List<String> imageUrls = new ArrayList<>();

    public Product() {}

    public Product(String name, String price, String location, String ratingSummary) {
        this(name, price, location, ratingSummary, null, new ArrayList<>());
    }

    public Product(String name, String price, String location, String ratingSummary,
                   String category, List<String> imageUrls) {
        this.name = name;
        this.price = price;
        this.location = location;
        this.ratingSummary = ratingSummary;
        this.category = category;
        this.imageUrls = imageUrls;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRatingSummary() { return ratingSummary; }
    public void setRatingSummary(String ratingSummary) { this.ratingSummary = ratingSummary; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    @Exclude
    public String getMainImageUrl() {
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
    }
}
