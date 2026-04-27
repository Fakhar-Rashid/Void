package com.example.avoid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cart implements Serializable {

    private String userId;
    private final List<CartProduct> items = new ArrayList<>();

    public Cart() {}

    public Cart(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartProduct> getItems() { return items; }

    public void addItem(Product product, String color, int quantity) {
        for (CartProduct existing : items) {
            if (existing.getProduct().getName().equals(product.getName())
                    && existing.getColor().equals(color)) {
                existing.setQuantity(existing.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartProduct(product, color, quantity));
    }

    public void removeItem(CartProduct item) {
        Iterator<CartProduct> it = items.iterator();
        while (it.hasNext()) {
            if (it.next() == item) { it.remove(); return; }
        }
    }

    public void clear() { items.clear(); }

    public int getTotalItemCount() {
        int n = 0;
        for (CartProduct item : items) n += item.getQuantity();
        return n;
    }

    public double getTotal() {
        double total = 0;
        for (CartProduct item : items) total += item.getPriceValue() * item.getQuantity();
        return total;
    }
}
