package com.example.avoid.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.UserRepository;
import com.example.avoid.UserSession;
import com.example.avoid.adapter.OrderLineAdapter;
import com.example.avoid.model.Address;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seller-side detail page for a single order. Shows the buyer (name/email/phone),
 * shipping address, payment method + paid/unpaid badge, the seller's earnings on this
 * order, and the seller's line items with status-update controls.
 */
public class SellerOrderDetailsFragment extends Fragment {

    private static final String ARG_ORDER = "order";

    private Order order;
    private RecyclerView itemsRecycler;
    private TextView sellerTotal;

    public static SellerOrderDetailsFragment newInstance(@NonNull Order order) {
        SellerOrderDetailsFragment fragment = new SellerOrderDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { close(); return; }
        order = (Order) args.getSerializable(ARG_ORDER);
        if (order == null) { close(); return; }

        ImageButton back = view.findViewById(R.id.sellerOrderDetailsBack);
        back.setOnClickListener(v -> close());

        bindOrder(view);
        bindBuyer(view);
        bindAddress(view);
        bindPayment(view);

        itemsRecycler = view.findViewById(R.id.sodItemsRecycler);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        bindItems();
    }

    private void bindOrder(View root) {
        String idShort = order.getOrderId() != null && order.getOrderId().length() > 8
                ? order.getOrderId().substring(0, 8).toUpperCase()
                : (order.getOrderId() != null ? order.getOrderId() : "—");
        ((TextView) root.findViewById(R.id.sodOrderId)).setText("Order #" + idShort);
        ((TextView) root.findViewById(R.id.sodOrderDate)).setText(order.getOrderDate() != null
                ? "Placed " + order.getOrderDate() : "Placed —");
    }

    private void bindBuyer(View root) {
        TextView name  = root.findViewById(R.id.sodBuyerName);
        TextView email = root.findViewById(R.id.sodBuyerEmail);
        TextView phone = root.findViewById(R.id.sodBuyerPhone);

        name.setText(notBlank(order.getBuyerName()) ? order.getBuyerName() : "Buyer");
        email.setText(notBlank(order.getBuyerEmail()) ? order.getBuyerEmail() : "—");
        if (notBlank(order.getBuyerPhone())) {
            phone.setVisibility(View.VISIBLE);
            phone.setText(order.getBuyerPhone());
        } else {
            phone.setVisibility(View.GONE);
        }
    }

    private void bindAddress(View root) {
        TextView addressView = root.findViewById(R.id.sodAddress);
        Address addr = order.getShippingAddress();
        addressView.setText(addr != null ? addr.getMultiLine() : "No address recorded");
    }

    private void bindPayment(View root) {
        TextView method = root.findViewById(R.id.sodPaymentMethod);
        TextView status = root.findViewById(R.id.sodPaymentStatus);
        sellerTotal = root.findViewById(R.id.sodSellerTotal);

        method.setText(order.getPaymentMethodLabel());
        status.setText(order.isPaid() ? "Paid" : "Pending — collect on delivery");

        String storeId = UserSession.getInstance().getCurrentUser().getId();
        sellerTotal.setText(String.format(Locale.US, "$%,.2f", order.getTotalForStore(storeId)));
    }

    private void bindItems() {
        String storeId = UserSession.getInstance().getCurrentUser().getId();
        List<OrderLineItem> myItems = new ArrayList<>();
        for (OrderLineItem item : order.getItems()) {
            if (storeId != null && storeId.equals(item.getStoreId())) myItems.add(item);
        }
        OrderLineAdapter adapter = new OrderLineAdapter(myItems, OrderLineAdapter.Mode.SELLER,
                this::advanceStatus);
        itemsRecycler.setAdapter(adapter);
    }

    private void advanceStatus(OrderLineItem item, int position) {
        Order.Status next = nextStatus(item.getStatus());
        if (next == null) return;

        long now = System.currentTimeMillis();
        item.setStatus(next);
        switch (next) {
            case PACKED:     item.setPackedAt(now); break;
            case ON_THE_WAY: item.setOnTheWayAt(now); break;
            case DELIVERED:  item.setDeliveredAt(now); break;
            default: break;
        }

        UserRepository.getInstance().saveOrder(order, new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order result) {
                if (!isAdded()) return;
                if (itemsRecycler.getAdapter() != null) {
                    itemsRecycler.getAdapter().notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to update status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static Order.Status nextStatus(Order.Status current) {
        if (current == null) return Order.Status.PACKED;
        switch (current) {
            case CONFIRMED:  return Order.Status.PACKED;
            case PACKED:     return Order.Status.ON_THE_WAY;
            case ON_THE_WAY: return Order.Status.DELIVERED;
            default:         return null;
        }
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private void close() {
        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
    }
}
