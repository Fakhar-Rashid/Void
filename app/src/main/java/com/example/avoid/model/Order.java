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
    private double totalAmount;
    private String orderDate;
    private long orderTimestamp;
    /** Stores that contributed at least one product to this order (denormalized for cheap querying). */
    private List<String> storeIds = new ArrayList<>();

    public Order() {}

    public Order(String orderId, String userId, List<OrderLineItem> items,
                 double totalAmount, String orderDate, long orderTimestamp, List<String> storeIds) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
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

    /** Lowest status across all line items — drives the overall progress indicator on the orders list. */
    @Exclude
    public Status getOverallStatus() {
        if (getItems().isEmpty()) return Status.CONFIRMED;
        Status lowest = Status.DELIVERED;
        for (OrderLineItem item : getItems()) {
            Status s = item.getStatus();
            if (s == null) s = Status.CONFIRMED;
            if (s.ordinal() < lowest.ordinal()) lowest = s;
        }
        return lowest;
    }

    @Exclude
    public boolean isFullyDelivered() {
        if (getItems().isEmpty()) return false;
        for (OrderLineItem item : getItems()) {
            if (item.getStatus() != Status.DELIVERED) return false;
        }
        return true;
    }

    @Exclude
    public boolean isFullyReviewed() {
        if (getItems().isEmpty()) return false;
        for (OrderLineItem item : getItems()) {
            if (!item.isReviewed()) return false;
        }
        return true;
    }
}
