package com.example.avoid.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.UserRepository;
import com.example.avoid.UserSession;
import com.example.avoid.adapter.OrderLineAdapter;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seller's incoming orders. Each row groups one order and renders this seller's items as a
 * read-only preview — the per-shipment status update lives on the details page so a single
 * tap can advance every item in the parcel together.
 */
public class SellerOrdersFragment extends Fragment {

    private RecyclerView ordersRecycler;
    private View emptyContainer;
    private TextView subtitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ordersRecycler = view.findViewById(R.id.sellerOrdersRecyclerView);
        emptyContainer = view.findViewById(R.id.sellerOrdersEmptyContainer);
        subtitle       = view.findViewById(R.id.sellerOrdersSubtitle);

        ordersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) return;

        UserRepository.getInstance().loadStoreOrders(user.getId(), this::bind);
    }

    private void bind(List<Order> orders) {
        if (getView() == null) return;
        String storeId = UserSession.getInstance().getCurrentUser().getId();

        if (orders.isEmpty()) {
            ordersRecycler.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
            subtitle.setText("");
            return;
        }

        ordersRecycler.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        subtitle.setText(String.format(Locale.US, "%d open order%s",
                orders.size(), orders.size() == 1 ? "" : "s"));
        ordersRecycler.setAdapter(new SellerOrderGroupAdapter(orders, storeId));
    }

    /** Outer adapter — one row per order, each rendering only this seller's items. */
    private class SellerOrderGroupAdapter extends RecyclerView.Adapter<SellerOrderGroupAdapter.GroupVH> {

        private final List<Order> orders;
        private final String storeId;

        SellerOrderGroupAdapter(List<Order> orders, String storeId) {
            this.orders = orders;
            this.storeId = storeId;
        }

        @NonNull
        @Override
        public GroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_seller_order_group, parent, false);
            return new GroupVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupVH holder, int position) {
            Order order = orders.get(position);

            String idShort = order.getOrderId() != null && order.getOrderId().length() > 8
                    ? order.getOrderId().substring(0, 8).toUpperCase()
                    : (order.getOrderId() != null ? order.getOrderId() : "—");
            holder.id.setText("Order #" + idShort);
            holder.date.setText(order.getOrderDate() != null ? order.getOrderDate() : "—");

            // Filter to the seller's items only.
            List<OrderLineItem> myItems = new ArrayList<>();
            for (OrderLineItem item : order.getItems()) {
                if (storeId != null && storeId.equals(item.getStoreId())) myItems.add(item);
            }

            holder.itemsRecycler.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            // Read-only items in SELLER mode — no per-item action button. Tap header → details.
            holder.itemsRecycler.setAdapter(new OrderLineAdapter(myItems, OrderLineAdapter.Mode.SELLER, null));

            holder.header.setOnClickListener(v -> openDetails(order));
        }

        @Override
        public int getItemCount() { return orders.size(); }

        class GroupVH extends RecyclerView.ViewHolder {
            final TextView id, date;
            final View header;
            final RecyclerView itemsRecycler;
            GroupVH(@NonNull View itemView) {
                super(itemView);
                id            = itemView.findViewById(R.id.sellerOrderGroupId);
                date          = itemView.findViewById(R.id.sellerOrderGroupDate);
                header        = itemView.findViewById(R.id.sellerOrderGroupHeader);
                itemsRecycler = itemView.findViewById(R.id.sellerOrderGroupItems);
            }
        }
    }

    private void openDetails(Order order) {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.sellerFragmentContainer, SellerOrderDetailsFragment.newInstance(order))
                .addToBackStack(null)
                .commit();
    }
}
