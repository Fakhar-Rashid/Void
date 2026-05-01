package com.example.avoid;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String chatId, productId, productName, storeId, storeName, buyerId, buyerName;
    private User currentUser;

    private RecyclerView messagesRecyclerView;
    private EditText etMessageInput;
    private ChatMessageAdapter adapter;
    private ValueEventListener messagesListener;

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

        if (chatId == null) {
            chatId = ChatRepository.getInstance().generateChatId(buyerId, storeId, productId);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        TextView tvChatSubtitle = findViewById(R.id.tvChatSubtitle);

        boolean isStoreOwner = currentUser.getId().equals(storeId);
        tvChatTitle.setText(isStoreOwner ? buyerName : storeName);
        tvChatSubtitle.setText(productName);

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        
        adapter = new ChatMessageAdapter(new ArrayList<>());
        messagesRecyclerView.setAdapter(adapter);

        etMessageInput = findViewById(R.id.etMessageInput);
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        messagesListener = ChatRepository.getInstance().listenForMessages(chatId, new ChatRepository.MessagesCallback() {
            @Override
            public void onMessagesUpdated(List<ChatMessage> messages) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
                }
                boolean isStoreOwner = currentUser.getId().equals(storeId);
                ChatRepository.getInstance().markChatAsRead(chatId, isStoreOwner);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
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
        if (messagesListener != null) {
            ChatRepository.getInstance().removeMessagesListener(chatId, messagesListener);
        }
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        private List<ChatMessage> messages;

        ChatMessageAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        void setMessages(List<ChatMessage> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (messages.get(position).getSenderId().equals(currentUser.getId())) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SENT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
                return new MessageViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
                return new MessageViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            MessageViewHolder msgHolder = (MessageViewHolder) holder;
            msgHolder.tvMessage.setText(msg.getText());
            
            String time = android.text.format.DateFormat.format("hh:mm a", msg.getTimestamp()).toString();
            msgHolder.tvTime.setText(time);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvTime;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}
