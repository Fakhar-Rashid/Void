package com.example.avoid.model;

public class CartProduct {

    private final Product product;
    private final String color;
    private int quantity;

    public CartProduct(Product product, String color, int quantity) {
        this.product = product;
        this.color = color;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public String getColor() { return color; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPriceValue() {
        String raw = product.getPrice().replace("$", "").replace(",", "").trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
