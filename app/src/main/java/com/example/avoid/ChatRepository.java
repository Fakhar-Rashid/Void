package com.example.avoid;

import androidx.annotation.NonNull;

import com.example.avoid.model.Chat;
import com.example.avoid.model.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    public void sendMessage(String chatId, ChatMessage message, Chat chatMetadata) {
        DatabaseReference chatRef = db.getReference("chats").child(chatId);
        DatabaseReference messagesRef = db.getReference("messages").child(chatId);
        DatabaseReference userChatsRef = db.getReference("userChats");

        // Push new message
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.setMessageId(messageId);
            messagesRef.child(messageId).setValue(message);
        }

        // Update chat metadata (last message, timestamp, participants info)
        chatMetadata.setChatId(chatId);
        chatMetadata.setLastMessage(message.getText());
        chatMetadata.setLastMessageTimestamp(message.getTimestamp());
        chatRef.setValue(chatMetadata);

        // Add to both users' chat lists
        userChatsRef.child(chatMetadata.getBuyerId()).child(chatId).setValue(true);
        userChatsRef.child(chatMetadata.getStoreId()).child(chatId).setValue(true);
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

    public ValueEventListener listenForUserChats(String userId, ChatListCallback callback) {
        DatabaseReference userChatsRef = db.getReference("userChats").child(userId);
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
        userChatsRef.addValueEventListener(listener);
        return listener;
    }

    public void removeUserChatsListener(String userId, ValueEventListener listener) {
        if (listener != null) {
            db.getReference("userChats").child(userId).removeEventListener(listener);
        }
    }
}
