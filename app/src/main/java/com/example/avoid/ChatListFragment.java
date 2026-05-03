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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.model.Chat;
import com.example.avoid.model.User;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private static final String ARG_IS_SELLER_MODE = "isSellerMode";
    
    private RecyclerView recyclerView;
    private TextView tvEmptyChats;
    private ChatListAdapter adapter;
    private ValueEventListener userChatsListener;
    private User currentUser;
    private boolean isSellerMode;

    public static ChatListFragment newInstance(boolean isSellerMode) {
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SELLER_MODE, isSellerMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isSellerMode = getArguments().getBoolean(ARG_IS_SELLER_MODE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        View btnBack = view.findViewById(R.id.btnBack);
        if (isSellerMode) {
            // Hide back button in seller tab mode
            btnBack.setVisibility(View.GONE);
        } else {
            btnBack.setOnClickListener(v -> requireActivity().finish());
        }

        
        recyclerView = view.findViewById(R.id.chatListRecyclerView);
        tvEmptyChats = view.findViewById(R.id.tvEmptyChats);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to view messages", Toast.LENGTH_SHORT).show();
            if (!isSellerMode) requireActivity().finish();
            return;
        }

        userChatsListener = ChatRepository.getInstance().listenForUserChats(currentUser.getId(), isSellerMode, new ChatRepository.ChatListCallback() {
            @Override
            public void onChatsLoaded(List<Chat> chats) {
                if (!isAdded()) return;
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
                if (isAdded()) Toast.makeText(requireContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentUser != null && userChatsListener != null) {
            ChatRepository.getInstance().removeUserChatsListener(currentUser.getId(), isSellerMode, userChatsListener);
        }
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

            // Determine if the current user is the buyer or the store owner for this thread
            boolean isStoreOwner = currentUser.getId().equals(chat.getStoreId());

            String displayName = isStoreOwner ? chat.getBuyerName() : chat.getStoreName();
            holder.chatName.setText(displayName != null ? displayName : "Unknown");

            // Reset avatar (recycler reuses ViewHolders).
            holder.chatAvatar.setImageResource(R.drawable.ic_profile);
            if (isStoreOwner) {
                // Seller's chat list shows the buyer's profile photo.
                if (chat.getBuyerId() != null) {
                    UserRepository.getInstance().loadUserProfileImage(chat.getBuyerId(),
                            new UserRepository.Callback<String>() {
                                @Override public void onSuccess(String url) {
                                    if (url != null && !url.isEmpty()) {
                                        com.bumptech.glide.Glide.with(holder.chatAvatar)
                                                .load(url)
                                                .placeholder(R.drawable.ic_profile)
                                                .error(R.drawable.ic_profile)
                                                .centerCrop()
                                                .into(holder.chatAvatar);
                                    }
                                }
                                @Override public void onFailure(@NonNull Exception e) {}
                            });
                }
            } else {
                // Buyer's chat list shows the store's logo.
                if (chat.getStoreId() != null) {
                    UserRepository.getInstance().loadStore(chat.getStoreId(),
                            new UserRepository.Callback<com.example.avoid.model.Store>() {
                                @Override public void onSuccess(com.example.avoid.model.Store store) {
                                    if (store != null && store.getLogoUrl() != null
                                            && !store.getLogoUrl().isEmpty()) {
                                        com.bumptech.glide.Glide.with(holder.chatAvatar)
                                                .load(store.getLogoUrl())
                                                .placeholder(R.drawable.ic_profile)
                                                .error(R.drawable.ic_profile)
                                                .centerCrop()
                                                .into(holder.chatAvatar);
                                    }
                                }
                                @Override public void onFailure(@NonNull Exception e) {}
                            });
                }
            }
            
            holder.chatProductName.setText(chat.getProductName() != null ? "Product: " + chat.getProductName() : "");

            String lastMsg = chat.getLastMessage();
            if (lastMsg == null || lastMsg.isEmpty()) {
                holder.chatLastMessage.setText("No messages yet");
            } else if (chat.getLastMessageSenderId() != null
                    && chat.getLastMessageSenderId().equals(currentUser.getId())) {
                holder.chatLastMessage.setText("You: " + lastMsg);
            } else {
                holder.chatLastMessage.setText(lastMsg);
            }

            if (chat.getLastMessageTimestamp() > 0) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(chat.getLastMessageTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                holder.chatTime.setText(timeAgo);
            } else {
                holder.chatTime.setText("");
            }

            int unreadCount = isStoreOwner ? chat.getUnreadCountStore() : chat.getUnreadCountBuyer();
            if (unreadCount > 0) {
                holder.chatUnreadCount.setVisibility(View.VISIBLE);
                holder.chatUnreadCount.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            } else {
                holder.chatUnreadCount.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
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
            android.widget.ImageView chatAvatar;
            TextView chatName, chatTime, chatProductName, chatLastMessage, chatUnreadCount;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                chatAvatar = itemView.findViewById(R.id.chatAvatar);
                chatName = itemView.findViewById(R.id.chatName);
                chatTime = itemView.findViewById(R.id.chatTime);
                chatProductName = itemView.findViewById(R.id.chatProductName);
                chatLastMessage = itemView.findViewById(R.id.chatLastMessage);
                chatUnreadCount = itemView.findViewById(R.id.chatUnreadCount);
            }
        }
    }
}
