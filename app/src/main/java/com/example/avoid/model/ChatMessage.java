package com.example.avoid.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChatMessage {
    private String messageId;
    private String senderId;
    private String text;
    private long timestamp;

    public ChatMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(ChatMessage.class)
    }

    public ChatMessage(String messageId, String senderId, String text, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
