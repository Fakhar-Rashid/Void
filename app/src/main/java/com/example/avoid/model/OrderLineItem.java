package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Locale;

/**
 * Order line item — snapshots product data at purchase time so deleted/edited products
 * don't change historical orders.
 */
public class OrderLineItem implements Serializable {

    private String productId;
    private String productName;
    private double productPrice;
    private String productImageUrl;
    private String color;
    private int quantity;

    public OrderLineItem() {}

    public OrderLineItem(String productId, String productName, double productPrice,
                         String productImageUrl, String color, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImageUrl = productImageUrl;
        this.color = color;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Exclude
    public double getLineTotal() { return productPrice * quantity; }

    @Exclude
    public String getDisplayPrice() {
        return String.format(Locale.US, "$%,.2f", productPrice);
    }
}
