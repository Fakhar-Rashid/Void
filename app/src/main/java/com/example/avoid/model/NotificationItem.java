package com.example.avoid.model;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * In-app notification record stored under {@code /notifications/{userId}/{pushKey}} in RTDB.
 * No FCM — the app listens to the path while it is open and renders a bell badge + list.
 */
public class NotificationItem implements Serializable {

    public static final String TYPE_ORDER_STATUS = "ORDER_STATUS";
    public static final String TYPE_ORDER_NEW    = "ORDER_NEW";
    public static final String TYPE_CHAT         = "CHAT";

    @Exclude
    private String id;

    private String type;
    private String title;
    private String body;
    private long timestamp;
    private boolean read;

    /** Optional deep-link hints — only the relevant ones are populated per type. */
    private String orderId;
    private String chatId;
    /** Sender uid — used to open the right chat when {@link #TYPE_CHAT}. */
    private String fromUserId;
    /** Sender display name — used as a chat-list fallback subtitle. */
    private String fromUserName;

    /** Denormalized chat metadata so the click handler can launch {@code ChatActivity} without
     *  another network round-trip. Only populated when {@link #TYPE_CHAT}. */
    private String productId;
    private String productName;
    private String buyerId;
    private String buyerName;
    private String storeId;
    private String storeName;

    public NotificationItem() {}

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
}
