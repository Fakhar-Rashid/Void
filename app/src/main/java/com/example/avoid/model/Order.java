package com.example.avoid.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {

    public enum Status {
        CONFIRMED, PACKED, ON_THE_WAY, DELIVERED
    }

    @DocumentId
    private String orderId;
    private String userId;
    private List<OrderLineItem> items = new ArrayList<>();
    private Status status;
    private double totalAmount;
    private String orderDate;
    private long orderTimestamp;
    private List<String> storeIds = new ArrayList<>();

    public Order() {}

    public Order(String orderId, String userId, List<OrderLineItem> items,
                 Status status, double totalAmount, String orderDate, long orderTimestamp, List<String> storeIds) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderTimestamp = orderTimestamp;
        this.storeIds = storeIds != null ? storeIds : new ArrayList<>();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<OrderLineItem> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }
    public void setItems(List<OrderLineItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public long getOrderTimestamp() { return orderTimestamp; }
    public void setOrderTimestamp(long orderTimestamp) { this.orderTimestamp = orderTimestamp; }

    public List<String> getStoreIds() {
        if (storeIds == null) storeIds = new ArrayList<>();
        return storeIds;
    }
    public void setStoreIds(List<String> storeIds) {
        this.storeIds = storeIds != null ? storeIds : new ArrayList<>();
    }

    @Exclude
    public OrderLineItem getFirstItem() {
        return getItems().isEmpty() ? null : getItems().get(0);
    }

    @Exclude
    public int getTotalItemCount() {
        int n = 0;
        for (OrderLineItem item : getItems()) n += item.getQuantity();
        return n;
    }
}
