package com.example.avoid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order implements Serializable {

    public enum Status {
        CONFIRMED, PACKED, ON_THE_WAY, DELIVERED
    }

    private String orderId;
    private String userId;
    private List<CartProduct> items = new ArrayList<>();
    private Status status;
    private double totalAmount;
    private String orderDate;

    public Order() {}

    public Order(String orderId, String userId, List<CartProduct> items,
                 Status status, double totalAmount, String orderDate) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
    }

    public static Order singleItem(String orderId, String userId, CartProduct item,
                                   Status status, String orderDate) {
        double total = item != null ? item.getPriceValue() * item.getQuantity() : 0;
        return new Order(orderId, userId,
                item != null ? Collections.singletonList(item) : Collections.emptyList(),
                status, total, orderDate);
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartProduct> getItems() { return items; }
    public void setItems(List<CartProduct> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public CartProduct getFirstItem() {
        return items.isEmpty() ? null : items.get(0);
    }

    public int getTotalItemCount() {
        int n = 0;
        for (CartProduct item : items) n += item.getQuantity();
        return n;
    }
}
