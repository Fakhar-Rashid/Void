package com.example.avoid.model;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Review implements Serializable {

    @DocumentId
    private String id;

    private String reviewerName;
    private float rating;
    private String date;
    private String comment;
    private String productId;
    private String orderId;
    /** Issue tags ticked by the buyer when rating < 3. Empty otherwise. */
    private List<String> issues = new ArrayList<>();
    private long createdAt;

    public List<String> getIssues() {
        if (issues == null) issues = new ArrayList<>();
        return issues;
    }
    public void setIssues(List<String> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public Review() {}

    public Review(String reviewerName, float rating, String date, String comment, String productId, String orderId) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.date = date;
        this.comment = comment;
        this.productId = productId;
        this.orderId = orderId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
