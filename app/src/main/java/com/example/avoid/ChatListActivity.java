package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.avoid.model.User;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmptyChats;
    private ChatListAdapter adapter;
    private ValueEventListener userChatsListener;
    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        setupSystemBars();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        recyclerView = findViewById(R.id.chatListRecyclerView);
        tvEmptyChats = findViewById(R.id.tvEmptyChats);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // We listen to userChats for the currentUser. 
        // If they are a buyer, they see buyer chats. If they are a store, they see store chats.
        // The repository adds to userChats/$userId/ for both buyerId and storeId!
        userChatsListener = ChatRepository.getInstance().listenForUserChats(currentUser.getId(), new ChatRepository.ChatListCallback() {
            @Override
            public void onChatsLoaded(List<Chat> chats) {
                if (chats.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyChats.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyChats.setVisibility(View.GONE);
                    adapter.setChats(chats);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ChatListActivity.this, "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentUser != null && userChatsListener != null) {
            ChatRepository.getInstance().removeUserChatsListener(currentUser.getId(), userChatsListener);
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

    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
        private List<Chat> chats;

        ChatListAdapter(List<Chat> chats) {
            this.chats = chats;
        }

        void setChats(List<Chat> chats) {
            this.chats = chats;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Chat chat = chats.get(position);
            
            // Determine if the current user is the buyer or the store owner
            boolean isStoreOwner = currentUser.getId().equals(chat.getStoreId());
            
            String displayName = isStoreOwner ? chat.getBuyerName() : chat.getStoreName();
            holder.chatName.setText(displayName != null ? displayName : "Unknown");
            
            holder.chatProductName.setText(chat.getProductName() != null ? "Product: " + chat.getProductName() : "");
            holder.chatLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "No messages yet");

            if (chat.getLastMessageTimestamp() > 0) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(chat.getLastMessageTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                holder.chatTime.setText(timeAgo);
            } else {
                holder.chatTime.setText("");
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("productId", chat.getProductId());
                intent.putExtra("productName", chat.getProductName());
                intent.putExtra("storeId", chat.getStoreId());
                intent.putExtra("storeName", chat.getStoreName());
                intent.putExtra("buyerId", chat.getBuyerId());
                intent.putExtra("buyerName", chat.getBuyerName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView chatName, chatTime, chatProductName, chatLastMessage;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                chatName = itemView.findViewById(R.id.chatName);
                chatTime = itemView.findViewById(R.id.chatTime);
                chatProductName = itemView.findViewById(R.id.chatProductName);
                chatLastMessage = itemView.findViewById(R.id.chatLastMessage);
            }
        }
    }
}
