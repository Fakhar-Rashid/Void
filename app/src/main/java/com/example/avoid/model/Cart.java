package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cart implements Serializable {

    private String userId;
    private List<CartItem> items = new ArrayList<>();

    public Cart() {}

    public Cart(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(String productId, String color, int quantity) {
        for (CartItem existing : getItems()) {
            if (existing.getProductId() != null
                    && existing.getProductId().equals(productId)
                    && safeEquals(existing.getColor(), color)) {
                existing.setQuantity(existing.getQuantity() + quantity);
                return;
            }
        }
        getItems().add(new CartItem(productId, color, quantity));
    }

    public void removeItem(CartItem item) {
        Iterator<CartItem> it = getItems().iterator();
        while (it.hasNext()) {
            if (it.next() == item) { it.remove(); return; }
        }
    }

    public void clear() { getItems().clear(); }

    @Exclude
    public int getTotalItemCount() {
        int n = 0;
        for (CartItem item : getItems()) n += item.getQuantity();
        return n;
    }

    @Exclude
    public boolean contains(String productId) {
        if (productId == null) return false;
        for (CartItem item : getItems()) {
            if (productId.equals(item.getProductId())) return true;
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
