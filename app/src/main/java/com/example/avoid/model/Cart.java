package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cart implements Serializable {

    private String userId;
    private List<CartProduct> items = new ArrayList<>();

    public Cart() {}

    public Cart(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartProduct> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
    public void setItems(List<CartProduct> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(Product product, String color, int quantity) {
        for (CartProduct existing : getItems()) {
            if (existing.getProduct().getName().equals(product.getName())
                    && existing.getColor().equals(color)) {
                existing.setQuantity(existing.getQuantity() + quantity);
                return;
            }
        }
        getItems().add(new CartProduct(product, color, quantity));
    }

    public void removeItem(CartProduct item) {
        Iterator<CartProduct> it = getItems().iterator();
        while (it.hasNext()) {
            if (it.next() == item) { it.remove(); return; }
        }
    }

    public void clear() { getItems().clear(); }

    @Exclude
    public int getTotalItemCount() {
        int n = 0;
        for (CartProduct item : getItems()) n += item.getQuantity();
        return n;
    }

    @Exclude
    public double getTotal() {
        double total = 0;
        for (CartProduct item : getItems()) total += item.getPriceValue() * item.getQuantity();
        return total;
    }
}
