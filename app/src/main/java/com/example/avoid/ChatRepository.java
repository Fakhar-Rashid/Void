package com.example.avoid;

import androidx.annotation.NonNull;

import com.example.avoid.model.Chat;
import com.example.avoid.model.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRepository {

    private static ChatRepository instance;
    private final FirebaseDatabase db;

    private ChatRepository() {
        // Use default database URL or specified one if needed.
        db = FirebaseDatabase.getInstance();
    }

    public static synchronized ChatRepository getInstance() {
        if (instance == null) {
            instance = new ChatRepository();
        }
        return instance;
    }

    public interface ChatListCallback {
        void onChatsLoaded(List<Chat> chats);
        void onError(Exception e);
    }

    public interface MessagesCallback {
        void onMessagesUpdated(List<ChatMessage> messages);
        void onError(Exception e);
    }
    
    public interface SingleChatCallback {
        void onChatLoaded(Chat chat);
        void onError(Exception e);
    }

    public interface UnreadCountCallback {
        void onUnreadCountUpdated(int totalUnread);
        void onError(Exception e);
    }

    // Generate a consistent chat ID based on the participants and product
    public String generateChatId(String buyerId, String storeId, String productId) {
        return buyerId + "_" + storeId + "_" + productId;
    }

    public void loadChat(String chatId, SingleChatCallback callback) {
        db.getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onChatLoaded(snapshot.getValue(Chat.class));
                } else {
                    callback.onChatLoaded(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void markChatAsRead(String chatId, boolean isStoreOwner) {
        DatabaseReference chatRef = db.getReference("chats").child(chatId);
        if (isStoreOwner) {
            chatRef.child("unreadCountStore").setValue(0);
        } else {
            chatRef.child("unreadCountBuyer").setValue(0);
        }
    }

    public void sendMessage(String chatId, ChatMessage message, Chat chatMetadata) {
        DatabaseReference chatRef = db.getReference("chats").child(chatId);
        DatabaseReference messagesRef = db.getReference("messages").child(chatId);
        DatabaseReference buyerChatsRef = db.getReference("buyerChats");
        DatabaseReference storeChatsRef = db.getReference("storeChats");

        // Push new message
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.setMessageId(messageId);
            messagesRef.child(messageId).setValue(message);
        }

        // Determine who sent the message
        boolean senderIsStore = message.getSenderId().equals(chatMetadata.getStoreId());

        // We fetch current chat to increment unread count properly without overwriting other fields.
        // If chat doesn't exist, we create it.
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat existingChat = snapshot.exists() ? snapshot.getValue(Chat.class) : chatMetadata;
                if (existingChat != null) {
                    existingChat.setLastMessage(message.getText());
                    existingChat.setLastMessageTimestamp(message.getTimestamp());
                    
                    if (senderIsStore) {
                        existingChat.setUnreadCountBuyer(existingChat.getUnreadCountBuyer() + 1);
                    } else {
                        existingChat.setUnreadCountStore(existingChat.getUnreadCountStore() + 1);
                    }
                    
                    chatRef.setValue(existingChat);

                    // Add to separated indices
                    buyerChatsRef.child(existingChat.getBuyerId()).child(chatId).setValue(true);
                    storeChatsRef.child(existingChat.getStoreId()).child(chatId).setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    public ValueEventListener listenForMessages(String chatId, MessagesCallback callback) {
        DatabaseReference messagesRef = db.getReference("messages").child(chatId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    ChatMessage msg = doc.getValue(ChatMessage.class);
                    if (msg != null) messages.add(msg);
                }
                callback.onMessagesUpdated(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        messagesRef.addValueEventListener(listener);
        return listener;
    }

    public void removeMessagesListener(String chatId, ValueEventListener listener) {
        if (listener != null) {
            db.getReference("messages").child(chatId).removeEventListener(listener);
        }
    }

    public ValueEventListener listenForUserChats(String userId, boolean isSellerMode, ChatListCallback callback) {
        String indexNode = isSellerMode ? "storeChats" : "buyerChats";
        DatabaseReference chatsIndexRef = db.getReference(indexNode).child(userId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> chatIds = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    chatIds.add(doc.getKey());
                }

                if (chatIds.isEmpty()) {
                    callback.onChatsLoaded(new ArrayList<>());
                    return;
                }

                // Fetch details for each chat
                db.getReference("chats").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {
                        List<Chat> chats = new ArrayList<>();
                        for (String chatId : chatIds) {
                            DataSnapshot chatSnap = chatsSnapshot.child(chatId);
                            if (chatSnap.exists()) {
                                Chat chat = chatSnap.getValue(Chat.class);
                                if (chat != null) chats.add(chat);
                            }
                        }
                        // Sort by timestamp descending
                        Collections.sort(chats, (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                        callback.onChatsLoaded(chats);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        chatsIndexRef.addValueEventListener(listener);
        return listener;
    }

    public void removeUserChatsListener(String userId, boolean isSellerMode, ValueEventListener listener) {
        if (listener != null) {
            String indexNode = isSellerMode ? "storeChats" : "buyerChats";
            db.getReference(indexNode).child(userId).removeEventListener(listener);
        }
    }

    public ValueEventListener listenForTotalUnreadCount(String userId, boolean isSellerMode, UnreadCountCallback callback) {
        String indexNode = isSellerMode ? "storeChats" : "buyerChats";
        DatabaseReference chatsIndexRef = db.getReference(indexNode).child(userId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> chatIds = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    chatIds.add(doc.getKey());
                }

                if (chatIds.isEmpty()) {
                    callback.onUnreadCountUpdated(0);
                    return;
                }

                db.getReference("chats").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {
                        int totalUnread = 0;
                        for (String chatId : chatIds) {
                            DataSnapshot chatSnap = chatsSnapshot.child(chatId);
                            if (chatSnap.exists()) {
                                Chat chat = chatSnap.getValue(Chat.class);
                                if (chat != null) {
                                    totalUnread += isSellerMode ? chat.getUnreadCountStore() : chat.getUnreadCountBuyer();
                                }
                            }
                        }
                        callback.onUnreadCountUpdated(totalUnread);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        chatsIndexRef.addValueEventListener(listener);
        return listener;
    }

    public void removeTotalUnreadCountListener(String userId, boolean isSellerMode, ValueEventListener listener) {
        if (listener != null) {
            String indexNode = isSellerMode ? "storeChats" : "buyerChats";
            db.getReference(indexNode).child(userId).removeEventListener(listener);
        }
    }
}
