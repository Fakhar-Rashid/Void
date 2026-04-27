package com.example.avoid;

import com.example.avoid.model.Cart;
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Order;
import com.example.avoid.model.Product;
import com.example.avoid.model.Settings;
import com.example.avoid.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In-memory session holding the current user. When auth lands, the login flow
 * builds a User (with cart and orders attached) and calls
 * {@link #setCurrentUser(User)} to install it here.
 */
public class UserSession {

    private static UserSession instance;
    private User currentUser;

    private UserSession() {
        currentUser = createMockUser();
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public void setCurrentUser(User user) { this.currentUser = user; }

    public void clear() { this.currentUser = null; }

    private static User createMockUser() {
        String userId = "mock-user-1";
        User user = new User(
                userId,
                "Fakhar Rashid",
                "rbashir6666@gmail.com",
                "+92 300 1234567",
                "Lahore, Pakistan",
                null,
                58.309,
                new Settings()
        );
        user.setCart(buildDemoCart(userId));
        user.setOrders(buildDemoOrders(userId));
        return user;
    }

    private static Cart buildDemoCart(String userId) {
        Cart cart = new Cart(userId);
        cart.getItems().add(new CartProduct(
                new Product("Xiamo 11T",     "$ 407.70", "North Jakarta", "4.8 | Sold 250+"), "White",  1));
        cart.getItems().add(new CartProduct(
                new Product("Redmi 9A",       "$ 348",    "South Jakarta", "4.8 | Sold 250+"), "Black",  1));
        cart.getItems().add(new CartProduct(
                new Product("Macbook Pro M1", "$ 1203",   "South Jakarta", "4.8 | Sold 250+"), "Silver", 1));
        return cart;
    }

    private static List<Order> buildDemoOrders(String userId) {
        return new ArrayList<>(Arrays.asList(
                Order.singleItem("VD-00481", userId,
                        new CartProduct(new Product("Sony WH-1000XM5", "$349.99", "Tokyo, JP", "4.9 (2.1k)"), "Midnight Black", 1),
                        Order.Status.DELIVERED, "Apr 10, 2026"),
                Order.singleItem("VD-00476", userId,
                        new CartProduct(new Product("MacBook Pro M3", "$1,999.00", "Cupertino, CA", "4.8 (987)"), "Space Gray", 1),
                        Order.Status.ON_THE_WAY, "Apr 14, 2026"),
                Order.singleItem("VD-00469", userId,
                        new CartProduct(new Product("Nike Air Max 270", "$129.99", "Portland, OR", "4.7 (3.4k)"), "Triple White", 2),
                        Order.Status.PACKED, "Apr 15, 2026"),
                Order.singleItem("VD-00463", userId,
                        new CartProduct(new Product("iPad Pro 12.9\"", "$1,099.00", "Cupertino, CA", "4.9 (1.2k)"), "Silver", 1),
                        Order.Status.CONFIRMED, "Apr 16, 2026"),
                Order.singleItem("VD-00455", userId,
                        new CartProduct(new Product("Samsung Galaxy S24 Ultra", "$1,299.00", "Seoul, KR", "4.8 (4.5k)"), "Titanium Gray", 1),
                        Order.Status.DELIVERED, "Apr 2, 2026"),
                Order.singleItem("VD-00448", userId,
                        new CartProduct(new Product("Keychron Q1 Pro", "$199.00", "Hong Kong", "4.9 (876)"), "Carbon Black", 1),
                        Order.Status.CONFIRMED, "Apr 16, 2026"),
                Order.singleItem("VD-00441", userId,
                        new CartProduct(new Product("Canon EOS R50", "$679.99", "Tokyo, JP", "4.7 (654)"), "White", 1),
                        Order.Status.ON_THE_WAY, "Apr 13, 2026"),
                Order.singleItem("VD-00434", userId,
                        new CartProduct(new Product("Dyson V15 Detect", "$749.99", "London, UK", "4.8 (1.8k)"), "Yellow/Nickel", 1),
                        Order.Status.PACKED, "Apr 15, 2026")
        ));
    }
}
