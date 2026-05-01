package com.example.avoid.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Product implements Serializable {

    @DocumentId
    private String id;

    private String name;
    private double price;
    private String description;
    private ProductCategory category;
    private Condition condition;
    private double weight;
    private WeightUnit weightUnit;

    /** Foreign key — points to stores/{storeId}. */
    private String storeId;

    /** Denormalized snapshots from the store doc — for fast display in lists. */
    private String storeName;
    private String location;

    private List<Color> availableColors = new ArrayList<>();
    private List<String> reviewIds = new ArrayList<>();
    private List<String> imageUrls = new ArrayList<>();

    private double rating;
    private int itemsSold;
    private int stock;
    private long createdAt;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public WeightUnit getWeightUnit() { return weightUnit; }
    public void setWeightUnit(WeightUnit weightUnit) { this.weightUnit = weightUnit; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<Color> getAvailableColors() {
        if (availableColors == null) availableColors = new ArrayList<>();
        return availableColors;
    }
    public void setAvailableColors(List<Color> availableColors) {
        this.availableColors = availableColors != null ? availableColors : new ArrayList<>();
    }

    public List<String> getReviewIds() {
        if (reviewIds == null) reviewIds = new ArrayList<>();
        return reviewIds;
    }
    public void setReviewIds(List<String> reviewIds) {
        this.reviewIds = reviewIds != null ? reviewIds : new ArrayList<>();
    }

    public List<String> getImageUrls() {
        if (imageUrls == null) imageUrls = new ArrayList<>();
        return imageUrls;
    }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getItemsSold() { return itemsSold; }
    public void setItemsSold(int itemsSold) { this.itemsSold = itemsSold; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // ------------ Derived display helpers (not persisted) ------------

    @Exclude
    public String getMainImageUrl() {
        return getImageUrls().isEmpty() ? null : getImageUrls().get(0);
    }

    @Exclude
    public String getDisplayPrice() {
        return String.format(Locale.US, "$%,.2f", price);
    }

    @Exclude
    public String getDisplayRating() {
        return String.format(Locale.US, "%.1f", rating);
    }

    @Exclude
    public String getDisplayRatingSummary() {
        if (itemsSold <= 0) return getDisplayRating();
        return getDisplayRating() + " | Sold " + formatSold(itemsSold);
    }

    @Exclude
    public String getDisplayWeight() {
        String number = (weight == Math.floor(weight))
                ? String.valueOf((long) weight)
                : String.format(Locale.US, "%.2f", weight).replaceAll("0+$", "").replaceAll("\\.$", "");
        if (weightUnit == null) return number;
        return number + " " + weightUnit.getSymbol();
    }

    @Exclude
    public boolean isOutOfStock() { return stock <= 0; }

    @Exclude
    public boolean isLowStock() { return stock > 0 && stock < 5; }

    private static String formatSold(int n) {
        if (n >= 1000) return (n / 100) / 10.0 + "k+";
        if (n >= 100)  return ((n / 100) * 100) + "+";
        return String.valueOf(n);
    }
}
