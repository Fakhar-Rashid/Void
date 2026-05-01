package com.example.avoid.model;

import java.io.Serializable;

/**
 * Cart line item — id-only. The product data (name, price, image) is fetched on demand
 * by the cart UI from the products collection.
 */
public class CartItem implements Serializable {

    private String productId;
    private String color;
    private int quantity;

    public CartItem() {}

    public CartItem(String productId, String color, int quantity) {
        this.productId = productId;
        this.color = color;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
