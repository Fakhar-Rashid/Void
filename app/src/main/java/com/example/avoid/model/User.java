package com.example.avoid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String profileImageUrl;
    private double balance;
    private Settings settings;
    private Cart cart;
    private List<Order> orders = new ArrayList<>();

    public User() {
        this.settings = new Settings();
        this.cart = new Cart();
    }

    public User(String id, String name, String email, String phone, String address,
                String profileImageUrl, double balance, Settings settings) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.profileImageUrl = profileImageUrl;
        this.balance = balance;
        this.settings = settings != null ? settings : new Settings();
        this.cart = new Cart(id);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public Settings getSettings() { return settings; }
    public void setSettings(Settings settings) {
        this.settings = settings != null ? settings : new Settings();
    }

    public Cart getCart() {
        if (cart == null) cart = new Cart(id);
        return cart;
    }
    public void setCart(Cart cart) {
        this.cart = cart != null ? cart : new Cart(id);
    }

    public List<Order> getOrders() {
        if (orders == null) orders = new ArrayList<>();
        return orders;
    }
    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
    }

    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
