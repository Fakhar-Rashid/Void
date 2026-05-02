package com.example.avoid;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.model.Chat;
import com.example.avoid.model.ChatMessage;
import com.example.avoid.model.User;
import com.google.firebase.database.ChildEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 50;

    private String chatId, productId, productName, storeId, storeName, buyerId, buyerName;
    private User currentUser;

    private RecyclerView messagesRecyclerView;
    private LinearLayoutManager layoutManager;
    private EditText etMessageInput;
    private ChatMessageAdapter adapter;
    private ChildEventListener messagesListener;

    private long oldestTimestamp = Long.MAX_VALUE;
    private boolean canLoadOlder = true;
    private boolean isLoadingOlder = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupSystemBars();

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatId = getIntent().getStringExtra("chatId");
        productId = getIntent().getStringExtra("productId");
        productName = getIntent().getStringExtra("productName");
        storeId = getIntent().getStringExtra("storeId");
        storeName = getIntent().getStringExtra("storeName");
        buyerId = getIntent().getStringExtra("buyerId");
        buyerName = getIntent().getStringExtra("buyerName");

        if (chatId == null && buyerId != null && storeId != null && productId != null) {
            chatId = ChatRepository.getInstance().generateChatId(buyerId, storeId, productId);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        TextView tvChatSubtitle = findViewById(R.id.tvChatSubtitle);

        boolean isStoreOwner = currentUser.getId().equals(storeId);
        tvChatTitle.setText(isStoreOwner ? buyerName : storeName);
        tvChatSubtitle.setText(productName);

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);

        adapter = new ChatMessageAdapter();
        messagesRecyclerView.setAdapter(adapter);
        messagesRecyclerView.addOnScrollListener(paginationScrollListener);

        etMessageInput = findViewById(R.id.etMessageInput);
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        // Ensure the chat doc exists before subscribing to messages. Security rules on
        // /messages/{chatId} reference the parent chat doc, and a brand-new conversation
        // wouldn't have one yet — without this pre-create, the first listener attach
        // gets canceled (you'd see "Failed to load messages").
        Chat seed = new Chat(chatId, productId, productName, buyerId, buyerName, storeId, storeName);
        ChatRepository.getInstance().ensureChatExists(chatId, seed, this::attachMessagesListener);
    }

    private void attachMessagesListener() {
        messagesListener = ChatRepository.getInstance().listenForRecentMessages(chatId, PAGE_SIZE,
                new ChatRepository.MessagesCallback() {
                    @Override
                    public void onMessageAdded(ChatMessage message) {
                        boolean inserted = adapter.addMessage(message);
                        if (!inserted) return;

                        if (message.getTimestamp() > 0 && message.getTimestamp() < oldestTimestamp) {
                            oldestTimestamp = message.getTimestamp();
                        }

                        // Reset unread for the receiving role on every incoming message.
                        boolean isStoreOwner = currentUser.getId().equals(storeId);
                        ChatRepository.getInstance().markChatAsRead(chatId, isStoreOwner);

                        // Auto-scroll only if the user is already at (or near) the bottom.
                        if (isNearBottom() || isOwnMessage(message)) {
                            messagesRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.e("ChatActivity", "messages listener canceled", e);
                        String detail = e != null && e.getLocalizedMessage() != null
                                ? e.getLocalizedMessage()
                                : "Failed to load messages";
                        Toast.makeText(ChatActivity.this, detail, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private final RecyclerView.OnScrollListener paginationScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (dy >= 0) return;                       // only when user scrolls up
            if (!canLoadOlder || isLoadingOlder) return;

            int firstVisible = layoutManager.findFirstVisibleItemPosition();
            if (firstVisible <= 5) loadOlder();
        }
    };

    private void loadOlder() {
        if (!canLoadOlder || isLoadingOlder) return;
        isLoadingOlder = true;
        long cursor = oldestTimestamp;

        ChatRepository.getInstance().loadOlderMessages(chatId, cursor, PAGE_SIZE,
                new ChatRepository.OlderMessagesCallback() {
                    @Override
                    public void onOlderLoaded(List<ChatMessage> messages) {
                        isLoadingOlder = false;
                        if (messages.isEmpty()) {
                            canLoadOlder = false;
                            return;
                        }
                        // Anchor the scroll on the previously-first item so the view doesn't jump.
                        int anchorPosition = adapter.getItemCount();
                        int prevTop = anchorPosition;
                        int offset = 0;
                        View topChild = layoutManager.findViewByPosition(layoutManager.findFirstVisibleItemPosition());
                        if (topChild != null) {
                            prevTop = layoutManager.findFirstVisibleItemPosition();
                            offset = topChild.getTop();
                        }

                        int added = adapter.prependMessages(messages);
                        oldestTimestamp = messages.get(0).getTimestamp();

                        layoutManager.scrollToPositionWithOffset(prevTop + added, offset);
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoadingOlder = false;
                    }
                });
    }

    private boolean isNearBottom() {
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        return lastVisible >= adapter.getItemCount() - 2;
    }

    private boolean isOwnMessage(ChatMessage msg) {
        return msg.getSenderId() != null && msg.getSenderId().equals(currentUser.getId());
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etMessageInput.setText("");

        ChatMessage msg = new ChatMessage(null, currentUser.getId(), text, System.currentTimeMillis());
        Chat chatMetadata = new Chat(chatId, productId, productName, buyerId, buyerName, storeId, storeName);
        ChatRepository.getInstance().sendMessage(chatId, msg, chatMetadata);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRecyclerView != null) {
            messagesRecyclerView.removeOnScrollListener(paginationScrollListener);
        }
        if (messagesListener != null) {
            ChatRepository.getInstance().removeMessagesListener(chatId, messagesListener);
        }
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content));
        if (insetsController != null) insetsController.setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    // ---- Adapter ------------------------------------------------------------------

    private class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        private final List<ChatMessage> messages = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        /** Inserts a message in chronological order. Returns true if it was actually new. */
        boolean addMessage(ChatMessage msg) {
            String id = msg.getMessageId();
            if (id != null && !ids.add(id)) return false; // dedup — message is already in our list.

            // Find insertion index (messages are kept ascending by timestamp).
            int idx = messages.size();
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).getTimestamp() <= msg.getTimestamp()) { idx = i + 1; break; }
                if (i == 0) idx = 0;
            }
            messages.add(idx, msg);
            notifyItemInserted(idx);
            return true;
        }

        /** Prepends a list of older messages (already sorted ascending). Returns count actually added. */
        int prependMessages(List<ChatMessage> older) {
            int added = 0;
            for (int i = older.size() - 1; i >= 0; i--) {
                ChatMessage m = older.get(i);
                String id = m.getMessageId();
                if (id != null && !ids.add(id)) continue;
                messages.add(0, m);
                added++;
            }
            if (added > 0) notifyItemRangeInserted(0, added);
            return added;
        }

        @Override
        public int getItemViewType(int position) {
            return isOwnMessage(messages.get(position)) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == VIEW_TYPE_SENT
                    ? R.layout.item_chat_message_sent
                    : R.layout.item_chat_message_received;
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            MessageViewHolder mh = (MessageViewHolder) holder;
            mh.tvMessage.setText(msg.getText());
            String time = android.text.format.DateFormat.format("hh:mm a", msg.getTimestamp()).toString();
            mh.tvTime.setText(time);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView tvMessage, tvTime;
            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}
