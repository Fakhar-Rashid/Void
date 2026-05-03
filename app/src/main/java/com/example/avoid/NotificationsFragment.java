package com.example.avoid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.model.NotificationItem;
import com.example.avoid.model.Order;
import com.example.avoid.seller.SellerOrderDetailsFragment;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recycler;
    private View emptyState;
    private final List<NotificationItem> items = new ArrayList<>();
    private NotificationAdapter adapter;
    @Nullable private ValueEventListener subscription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton back = view.findViewById(R.id.notificationsBack);
        back.setOnClickListener(v -> {
            if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
        });

        recycler   = view.findViewById(R.id.notificationsRecyclerView);
        emptyState = view.findViewById(R.id.notificationsEmpty);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(items, this::handleClick);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        setBottomChromeVisible(false);

        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null) return;
        String uid = session.getCurrentUser().getId();
        if (uid == null) return;

        subscription = NotificationRepository.getInstance().listen(uid,
                (all, unread) -> {
                    if (!isAdded()) return;
                    items.clear();
                    items.addAll(all);
                    adapter.notifyDataSetChanged();
                    emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                });

        // Mark as read once visible — the user has seen them.
        NotificationRepository.getInstance().markAllAsRead(uid);
    }

    @Override
    public void onStop() {
        super.onStop();
        setBottomChromeVisible(true);

        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() != null && session.getCurrentUser().getId() != null) {
            NotificationRepository.getInstance().stopListening(
                    session.getCurrentUser().getId(), subscription);
        }
        subscription = null;
    }

    /** Hide/show the host activity's bottom nav and FAB while the notifications screen is up. */
    private void setBottomChromeVisible(boolean visible) {
        Activity a = getActivity();
        if (a == null) return;
        int v = visible ? View.VISIBLE : View.GONE;
        applyVisibility(a, R.id.tabLayout, v);          // buyer MainActivity bottom nav
        applyVisibility(a, R.id.sellerBottomBar, v);    // seller bottom nav
        applyVisibility(a, R.id.sellerFab, v);          // seller floating add-product button
    }

    private static void applyVisibility(Activity a, int id, int visibility) {
        View view = a.findViewById(id);
        if (view != null) view.setVisibility(visibility);
    }

    private void handleClick(NotificationItem n) {
        if (n.getType() == null) return;
        switch (n.getType()) {
            case NotificationItem.TYPE_CHAT:
                openChat(n);
                break;
            case NotificationItem.TYPE_ORDER_NEW:
                openOrderForSeller(n);
                break;
            case NotificationItem.TYPE_ORDER_STATUS:
                openOrderForBuyer(n);
                break;
            default:
                break;
        }
    }

    private void openChat(NotificationItem n) {
        if (n.getChatId() == null) return;
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("chatId",      n.getChatId());
        intent.putExtra("productId",   n.getProductId());
        intent.putExtra("productName", n.getProductName());
        intent.putExtra("buyerId",     n.getBuyerId());
        intent.putExtra("buyerName",   n.getBuyerName());
        intent.putExtra("storeId",     n.getStoreId());
        intent.putExtra("storeName",   n.getStoreName());
        startActivity(intent);
    }

    private void openOrderForSeller(NotificationItem n) {
        if (n.getOrderId() == null) return;
        UserRepository.getInstance().loadOrder(n.getOrderId(), new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order order) {
                if (!isAdded() || order == null) return;
                int containerId = pickContainerId();
                if (containerId == 0) return;
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(containerId, SellerOrderDetailsFragment.newInstance(order))
                        .addToBackStack(null)
                        .commit();
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Couldn't open the order", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOrderForBuyer(NotificationItem n) {
        if (n.getOrderId() == null) return;
        UserRepository.getInstance().loadOrder(n.getOrderId(), new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order order) {
                if (!isAdded() || order == null) return;
                int containerId = pickContainerId();
                if (containerId == 0) return;
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(containerId, OrderDetailsFragment.newInstance(order))
                        .addToBackStack(null)
                        .commit();
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Couldn't open the order", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Same container resolution as {@link NotificationBellButton}. */
    private int pickContainerId() {
        FragmentActivity activity = getActivity() instanceof FragmentActivity
                ? (FragmentActivity) getActivity() : null;
        if (activity == null) return 0;
        if (activity.findViewById(R.id.sellerFragmentContainer) != null) return R.id.sellerFragmentContainer;
        if (activity.findViewById(R.id.fragment_container) != null) return R.id.fragment_container;
        return 0;
    }

    static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        interface OnClick { void onClick(NotificationItem n); }

        private final List<NotificationItem> data;
        private final OnClick onClick;

        NotificationAdapter(List<NotificationItem> data, OnClick onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NotificationItem n = data.get(position);
            holder.title.setText(n.getTitle() != null ? n.getTitle() : "Notification");
            holder.body.setText(n.getBody() != null ? n.getBody() : "");
            holder.time.setText(n.getTimestamp() > 0
                    ? DateUtils.getRelativeTimeSpanString(n.getTimestamp(),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                    : "");
            holder.icon.setImageResource(iconFor(n.getType()));
            holder.unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
            holder.itemView.setOnClickListener(v -> onClick.onClick(n));
        }

        @Override
        public int getItemCount() { return data.size(); }

        private static int iconFor(String type) {
            if (type == null) return R.drawable.ic_bell;
            switch (type) {
                case NotificationItem.TYPE_CHAT:         return R.drawable.ic_chat;
                case NotificationItem.TYPE_ORDER_NEW:    return R.drawable.ic_orders;
                case NotificationItem.TYPE_ORDER_STATUS: return R.drawable.ic_orders;
                default:                                 return R.drawable.ic_bell;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final View unreadDot;
            final TextView title, body, time;
            VH(@NonNull View v) {
                super(v);
                icon      = v.findViewById(R.id.notificationIcon);
                unreadDot = v.findViewById(R.id.notificationUnreadDot);
                title     = v.findViewById(R.id.notificationTitle);
                body      = v.findViewById(R.id.notificationBody);
                time      = v.findViewById(R.id.notificationTime);
            }
        }
    }
}
