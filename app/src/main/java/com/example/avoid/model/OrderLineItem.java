package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Locale;

/**
 * Order line item — snapshots product data at purchase time so deleted/edited products
 * don't change historical orders. Each line item carries its OWN status + timeline so
 * different products in the same order can ship/arrive on different schedules, and so
 * sellers only see and act on the items from their store.
 */
public class OrderLineItem implements Serializable {

    private String productId;
    private String productName;
    private double productPrice;
    private String productImageUrl;
    private String color;
    private int quantity;
    private String storeId;

    /** Per-item status. Seller can advance only the items in their store. */
    private Order.Status status = Order.Status.CONFIRMED;

    /** Server-recorded timestamps for each transition. 0 = not yet reached. */
    private long confirmedAt;
    private long packedAt;
    private long onTheWayAt;
    private long deliveredAt;

    /** Buyer-side review state for this specific product within this order. */
    private boolean reviewed;

    public OrderLineItem() {}

    public OrderLineItem(String productId, String productName, double productPrice,
                         String productImageUrl, String color, int quantity, String storeId) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productImageUrl = productImageUrl;
        this.color = color;
        this.quantity = quantity;
        this.storeId = storeId;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public Order.Status getStatus() {
        return status != null ? status : Order.Status.CONFIRMED;
    }
    public void setStatus(Order.Status status) {
        this.status = status != null ? status : Order.Status.CONFIRMED;
    }

    public long getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(long confirmedAt) { this.confirmedAt = confirmedAt; }

    public long getPackedAt() { return packedAt; }
    public void setPackedAt(long packedAt) { this.packedAt = packedAt; }

    public long getOnTheWayAt() { return onTheWayAt; }
    public void setOnTheWayAt(long onTheWayAt) { this.onTheWayAt = onTheWayAt; }

    public long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(long deliveredAt) { this.deliveredAt = deliveredAt; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    @Exclude
    public double getLineTotal() { return productPrice * quantity; }

    @Exclude
    public String getDisplayPrice() {
        return String.format(Locale.US, "$%,.2f", productPrice);
    }

    /** Returns the timestamp of the *current* status transition, or 0 if not recorded. */
    @Exclude
    public long getStatusTimestamp() {
        switch (getStatus()) {
            case CONFIRMED:  return confirmedAt;
            case PACKED:     return packedAt;
            case ON_THE_WAY: return onTheWayAt;
            case DELIVERED:  return deliveredAt;
            default:         return 0;
        }
    }
}
