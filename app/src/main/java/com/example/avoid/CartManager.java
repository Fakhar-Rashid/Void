package com.example.avoid;

import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Product;

import java.util.ArrayList;
import java.util.List;

public class CartManager {

    private static CartManager instance;
    private final List<CartProduct> cartItems;

    private CartManager() {
        cartItems = new ArrayList<>();
        // Add initial dummy data to keep the existing demo look
        cartItems.add(new CartProduct(
                new Product("Xiamo 11T",     "$ 407.70", "North Jakarta", "4.8 | Sold 250+"), "White",  1));
        cartItems.add(new CartProduct(
                new Product("Redmi 9A",       "$ 348",    "South Jakarta", "4.8 | Sold 250+"), "Black",  1));
        cartItems.add(new CartProduct(
                new Product("Macbook Pro M1", "$ 1203",   "South Jakarta", "4.8 | Sold 250+"), "Silver", 1));
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public List<CartProduct> getCartItems() {
        return cartItems;
    }

    public void addProduct(Product product, String color, int quantity) {
        for (CartProduct item : cartItems) {
            if (item.getProduct().getName().equals(product.getName()) && item.getColor().equals(color)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        cartItems.add(new CartProduct(product, color, quantity));
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartProduct item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }
}
