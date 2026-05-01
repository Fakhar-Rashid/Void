package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class CartProduct implements Serializable {

    private Product product;
    private String color;
    private int quantity;

    public CartProduct() {}

    public CartProduct(Product product, String color, int quantity) {
        this.product = product;
        this.color = color;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Exclude
    public double getPriceValue() {
        if (product == null || product.getPrice() == null) return 0.0;
        String raw = product.getPrice().replace("$", "").replace(",", "").trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
