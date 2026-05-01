package com.example.avoid.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Chat {
    private String chatId;
    private String productId;
    private String productName;
    private String buyerId;
    private String buyerName;
    private String storeId;
    private String storeName;
    private String lastMessage;
    private long lastMessageTimestamp;

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Chat.class)
    }

    public Chat(String chatId, String productId, String productName, String buyerId, String buyerName, String storeId, String storeName) {
        this.chatId = chatId;
        this.productId = productId;
        this.productName = productName;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.storeId = storeId;
        this.storeName = storeName;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

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

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }
}
