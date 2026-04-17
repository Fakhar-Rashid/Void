package com.example.avoid.model;

public class Order {

    public enum Status {
        CONFIRMED, PACKED, ON_THE_WAY, DELIVERED
    }

    private final String orderId;
    private final CartProduct item;
    private final Status status;
    private final String orderDate;

    public Order(String orderId, CartProduct item, Status status, String orderDate) {
        this.orderId = orderId;
        this.item = item;
        this.status = status;
        this.orderDate = orderDate;
    }

    public String getOrderId() { return orderId; }
    public CartProduct getItem() { return item; }
    public Status getStatus() { return status; }
    public String getOrderDate() { return orderDate; }
}
