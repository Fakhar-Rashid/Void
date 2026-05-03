package com.example.avoid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.avoid.model.Chat;
import com.example.avoid.model.ChatMessage;
import com.example.avoid.model.NotificationItem;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RTDB-backed chat. One chat per (buyer, seller, product) tuple.
 *
 * <pre>
 *   /chats/{chatId}                ← metadata: participants, last message, unread counters
 *   /messages/{chatId}/{msgId}    ← message log, ordered by timestamp
 * </pre>
 *
 * Architectural notes:
 * <ul>
 *   <li>Single source of truth at <code>/chats/{chatId}</code> — no secondary index nodes
 *       to drift out of sync. Buyer/seller views are different queries on the same data.</li>
 *   <li>Chat metadata mutations go through <code>runTransaction</code>, so unread counters
 *       and "last message" never lose updates when two messages land at the same instant.</li>
 *   <li>Message loading is bounded: open the chat, you get the latest N messages live;
 *       scroll to the top, the activity calls {@link #loadOlderMessages} for a single-shot page.</li>
 * </ul>
 *
 * <strong>Realtime Database security rules (recommended)</strong>:
 * <pre>{@code
 * {
 *   "rules": {
 *     "chats": {
 *       ".indexOn": ["buyerId", "storeId"],
 *       "$chatId": {
 *         ".read":  "auth != null && (data.child('buyerId').val() == auth.uid || data.child('storeId').val() == auth.uid)",
 *         ".write": "auth != null && (data.child('buyerId').val() == auth.uid || data.child('storeId').val() == auth.uid || newData.child('buyerId').val() == auth.uid || newData.child('storeId').val() == auth.uid)"
 *       }
 *     },
 *     "messages": {
 *       "$chatId": {
 *         ".read":  "auth != null && (root.child('chats').child($chatId).child('buyerId').val() == auth.uid || root.child('chats').child($chatId).child('storeId').val() == auth.uid)",
 *         ".write": "auth != null && (root.child('chats').child($chatId).child('buyerId').val() == auth.uid || root.child('chats').child($chatId).child('storeId').val() == auth.uid)"
 *       }
 *     }
 *   }
 * }
 * }</pre>
 */
public class ChatRepository {

    public static final int DEFAULT_MESSAGE_LIMIT = 50;

    private static ChatRepository instance;
    private final FirebaseDatabase db;

    private ChatRepository() {
        db = FirebaseDatabase.getInstance();
    }

    public static synchronized ChatRepository getInstance() {
        if (instance == null) instance = new ChatRepository();
        return instance;
    }

    // ---- Callbacks -----------------------------------------------------------------

    public interface ChatListCallback {
        void onChatsLoaded(List<Chat> chats);
        void onError(Exception e);
    }

    public interface MessagesCallback {
        /** Fires for every existing message on attach (oldest → newest), then for each new one. */
        void onMessageAdded(ChatMessage message);
        void onError(Exception e);
    }

    public interface OlderMessagesCallback {
        /** Single-shot. List is sorted oldest → newest. Empty when there are no more. */
        void onOlderLoaded(List<ChatMessage> messages);
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

    // ---- Chat metadata -------------------------------------------------------------

    /** Deterministic chat id from the (buyer, store, product) tuple. */
    public String generateChatId(String buyerId, String storeId, String productId) {
        return buyerId + "_" + storeId + "_" + productId;
    }

    public void loadChat(String chatId, SingleChatCallback callback) {
        db.getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onChatLoaded(snapshot.exists() ? snapshot.getValue(Chat.class) : null);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    /**
     * Reset the unread counter for the given role. Caller decides which role they are
     * based on whether {@code currentUserId.equals(chat.storeId)}.
     */
    public void markChatAsRead(String chatId, boolean isStoreOwner) {
        DatabaseReference chatRef = db.getReference("chats").child(chatId);
        if (isStoreOwner) chatRef.child("unreadCountStore").setValue(0);
        else              chatRef.child("unreadCountBuyer").setValue(0);
    }

    // ---- Sending -------------------------------------------------------------------

    /**
     * Persist a message and atomically update the chat's last-message metadata + unread counter.
     * Uses {@link DatabaseReference#runTransaction(Transaction.Handler)} so two simultaneous
     * sends can't lose an unread increment or step on each other's last-message field.
     */
    public void sendMessage(String chatId, ChatMessage message, Chat chatMetadata) {
        DatabaseReference messagesRef = db.getReference("messages").child(chatId);
        DatabaseReference chatRef = db.getReference("chats").child(chatId);

        final String messageId = messagesRef.push().getKey();
        if (messageId == null) return;
        message.setMessageId(messageId);

        final String senderId = message.getSenderId();
        final boolean senderIsStore = senderId != null && senderId.equals(chatMetadata.getStoreId());
        final String text = message.getText();
        final long timestamp = message.getTimestamp();
        final ChatMessage messageToPersist = message;

        // Step 1 — atomic chat-doc upsert.
        chatRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Chat chat = currentData.getValue(Chat.class);
                if (chat == null) chat = chatMetadata;
                if (chat.getChatId() == null) chat.setChatId(chatId);

                chat.setLastMessage(text);
                chat.setLastMessageTimestamp(timestamp);
                chat.setLastMessageSenderId(senderId);

                if (senderIsStore) chat.setUnreadCountBuyer(chat.getUnreadCountBuyer() + 1);
                else               chat.setUnreadCountStore(chat.getUnreadCountStore() + 1);

                currentData.setValue(chat);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                // Step 2 — only after the chat doc exists do we write the message.
                // This makes the order safe under restrictive security rules where
                // /messages/{chatId} is gated on the parent chat being readable.
                if (committed) {
                    messagesRef.child(messageId).setValue(messageToPersist);
                    fireChatNotification(chatId, chatMetadata, messageToPersist);
                } else {
                    android.util.Log.e("ChatRepository", "Chat doc upsert failed", error != null ? error.toException() : null);
                }
            }
        });
    }

    /** Fire-and-forget: ping the OTHER participant about an incoming message. */
    private void fireChatNotification(String chatId, Chat chat, ChatMessage message) {
        if (chat == null || message == null) return;
        String senderId = message.getSenderId();
        if (senderId == null) return;

        boolean senderIsStore = senderId.equals(chat.getStoreId());
        String recipientId   = senderIsStore ? chat.getBuyerId() : chat.getStoreId();
        if (recipientId == null) return;

        String senderName = senderIsStore
                ? (chat.getStoreName() != null ? chat.getStoreName() : "Store")
                : (chat.getBuyerName() != null ? chat.getBuyerName() : "Customer");

        NotificationItem n = new NotificationItem();
        n.setType(NotificationItem.TYPE_CHAT);
        n.setTitle("New message from " + senderName);
        String preview = message.getText() != null ? message.getText() : "";
        if (preview.length() > 120) preview = preview.substring(0, 120) + "…";
        n.setBody(preview);
        n.setChatId(chatId);
        n.setFromUserId(senderId);
        n.setFromUserName(senderName);
        n.setProductId(chat.getProductId());
        n.setProductName(chat.getProductName());
        n.setBuyerId(chat.getBuyerId());
        n.setBuyerName(chat.getBuyerName());
        n.setStoreId(chat.getStoreId());
        n.setStoreName(chat.getStoreName());
        NotificationRepository.getInstance().send(recipientId, n);
    }

    /**
     * Idempotent: ensures /chats/{chatId} exists with the given metadata. If it already exists,
     * it's left untouched. Useful for pre-creating the chat doc when {@link ChatActivity}
     * first opens — guarantees that subsequent reads of /messages/{chatId} (whose security
     * rule references the parent chat doc) succeed for a brand-new conversation.
     */
    public void ensureChatExists(String chatId, Chat chatMetadata, @Nullable Runnable onReady) {
        DatabaseReference chatRef = db.getReference("chats").child(chatId);
        chatRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    Chat fresh = chatMetadata;
                    if (fresh.getChatId() == null) fresh.setChatId(chatId);
                    currentData.setValue(fresh);
                }
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (onReady != null) onReady.run();
            }
        });
    }

    // ---- Messages ------------------------------------------------------------------

    /**
     * Live tail of the latest {@code limit} messages. Returns a listener that the caller
     * must remove via {@link #removeMessagesListener(String, ChildEventListener)} on cleanup.
     */
    public ChildEventListener listenForRecentMessages(String chatId, int limit, MessagesCallback callback) {
        Query q = db.getReference("messages").child(chatId)
                .orderByChild("timestamp")
                .limitToLast(limit);

        ChildEventListener listener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String prev) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg == null) return;
                if (msg.getMessageId() == null) msg.setMessageId(snapshot.getKey());
                callback.onMessageAdded(msg);
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        q.addChildEventListener(listener);
        return listener;
    }

    /**
     * Single-shot fetch of older messages — use this for scroll-up pagination.
     * Returns up to {@code limit} messages strictly older than {@code beforeTimestamp}.
     */
    public void loadOlderMessages(String chatId, long beforeTimestamp, int limit, OlderMessagesCallback callback) {
        Query q = db.getReference("messages").child(chatId)
                .orderByChild("timestamp")
                .endBefore((double) beforeTimestamp)
                .limitToLast(limit);

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> msgs = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    ChatMessage m = doc.getValue(ChatMessage.class);
                    if (m == null) continue;
                    if (m.getMessageId() == null) m.setMessageId(doc.getKey());
                    msgs.add(m);
                }
                Collections.sort(msgs, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                callback.onOlderLoaded(msgs);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void removeMessagesListener(String chatId, ChildEventListener listener) {
        if (listener != null) {
            db.getReference("messages").child(chatId).removeEventListener(listener);
        }
    }

    // ---- Chat lists (buyer / seller views) ----------------------------------------

    public ValueEventListener listenForUserChats(String userId, boolean isSellerMode, ChatListCallback callback) {
        Query query = userChatsQuery(userId, isSellerMode);

        ValueEventListener listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Chat> chats = new ArrayList<>();
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Chat chat = doc.getValue(Chat.class);
                    if (chat == null) continue;
                    if (chat.getChatId() == null) chat.setChatId(doc.getKey());
                    chats.add(chat);
                }
                Collections.sort(chats,
                        (a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                callback.onChatsLoaded(chats);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeUserChatsListener(String userId, boolean isSellerMode, ValueEventListener listener) {
        if (listener != null) userChatsQuery(userId, isSellerMode).removeEventListener(listener);
    }

    public ValueEventListener listenForTotalUnreadCount(String userId, boolean isSellerMode, UnreadCountCallback callback) {
        Query query = userChatsQuery(userId, isSellerMode);

        ValueEventListener listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUnread = 0;
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Chat chat = doc.getValue(Chat.class);
                    if (chat == null) continue;
                    totalUnread += isSellerMode ? chat.getUnreadCountStore() : chat.getUnreadCountBuyer();
                }
                callback.onUnreadCountUpdated(totalUnread);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };
        query.addValueEventListener(listener);
        return listener;
    }

    public void removeTotalUnreadCountListener(String userId, boolean isSellerMode, ValueEventListener listener) {
        if (listener != null) userChatsQuery(userId, isSellerMode).removeEventListener(listener);
    }

    private Query userChatsQuery(String userId, boolean isSellerMode) {
        String field = isSellerMode ? "storeId" : "buyerId";
        return db.getReference("chats").orderByChild(field).equalTo(userId);
    }
}
