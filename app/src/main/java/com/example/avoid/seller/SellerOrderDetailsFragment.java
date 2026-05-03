package com.example.avoid.seller;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.NotificationRepository;
import com.example.avoid.R;
import com.example.avoid.UserRepository;
import com.example.avoid.UserSession;
import com.example.avoid.adapter.OrderLineAdapter;
import com.example.avoid.model.Address;
import com.example.avoid.model.NotificationItem;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seller-side detail page for a single order. Status is per-shipment now (not per-item) — one
 * "Mark as …" button advances every line item belonging to this seller in lockstep.
 */
public class SellerOrderDetailsFragment extends Fragment {

    private static final String ARG_ORDER = "order";

    private Order order;
    private RecyclerView itemsRecycler;
    private TextView sellerTotal;
    private TextView statusText;
    private MaterialButton advanceButton;
    private View seg1, seg2, seg3, seg4;
    private final List<OrderLineItem> myItems = new ArrayList<>();

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
        bindShipment(view);

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

    private void bindShipment(View root) {
        statusText    = root.findViewById(R.id.sodStatusText);
        advanceButton = root.findViewById(R.id.sodAdvanceButton);
        seg1 = root.findViewById(R.id.sodSeg1);
        seg2 = root.findViewById(R.id.sodSeg2);
        seg3 = root.findViewById(R.id.sodSeg3);
        seg4 = root.findViewById(R.id.sodSeg4);
    }

    private void bindItems() {
        String storeId = UserSession.getInstance().getCurrentUser().getId();
        myItems.clear();
        for (OrderLineItem item : order.getItems()) {
            if (storeId != null && storeId.equals(item.getStoreId())) myItems.add(item);
        }
        OrderLineAdapter adapter = new OrderLineAdapter(myItems, OrderLineAdapter.Mode.SELLER, null);
        itemsRecycler.setAdapter(adapter);
        renderShipmentChrome();
    }

    /** Refresh progress + status text + button label from the current shipment status. */
    private void renderShipmentChrome() {
        Order.Status current = currentShipmentStatus();
        paintProgress(requireContext(), current, seg1, seg2, seg3, seg4);
        statusText.setText(buildStatusText(current));

        Order.Status next = nextStatus(current);
        if (next == null) {
            advanceButton.setVisibility(View.GONE);
            return;
        }
        advanceButton.setVisibility(View.VISIBLE);
        advanceButton.setText(buttonLabelFor(next));
        advanceButton.setOnClickListener(v -> advanceShipment());
    }

    private Order.Status currentShipmentStatus() {
        Order.Status lowest = Order.Status.DELIVERED;
        for (OrderLineItem item : myItems) {
            Order.Status s = item.getStatus() != null ? item.getStatus() : Order.Status.CONFIRMED;
            if (s.ordinal() < lowest.ordinal()) lowest = s;
        }
        return myItems.isEmpty() ? Order.Status.CONFIRMED : lowest;
    }

    private void advanceShipment() {
        Order.Status next = nextStatus(currentShipmentStatus());
        if (next == null) return;
        long now = System.currentTimeMillis();
        for (OrderLineItem item : myItems) {
            item.setStatus(next);
            switch (next) {
                case PACKED:     item.setPackedAt(now); break;
                case ON_THE_WAY: item.setOnTheWayAt(now); break;
                case DELIVERED:  item.setDeliveredAt(now); break;
                default: break;
            }
        }
        advanceButton.setEnabled(false);
        UserRepository.getInstance().saveOrder(order, new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order result) {
                if (!isAdded()) return;
                advanceButton.setEnabled(true);
                renderShipmentChrome();
                if (itemsRecycler.getAdapter() != null) {
                    itemsRecycler.getAdapter().notifyDataSetChanged();
                }
                notifyBuyerOfShipmentStatus(order, myItems, next);
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                advanceButton.setEnabled(true);
                Toast.makeText(requireContext(),
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to update status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** One notification per shipment per status bump (replaces per-item pings). */
    static void notifyBuyerOfShipmentStatus(Order order, List<OrderLineItem> shipmentItems, Order.Status next) {
        if (order == null || order.getUserId() == null || next == null) return;
        String storeName = null;
        for (OrderLineItem item : shipmentItems) {
            if (item.getStoreId() != null) {
                storeName = item.getStoreId(); // fallback: use id when name unknown
                break;
            }
        }

        NotificationItem n = new NotificationItem();
        n.setType(NotificationItem.TYPE_ORDER_STATUS);
        n.setTitle(statusTitle(next));
        int count = shipmentItems.size();
        String itemsLabel = count == 1
                ? (shipmentItems.get(0).getProductName() != null
                        ? shipmentItems.get(0).getProductName() : "Your item")
                : count + " items in your order";
        n.setBody(itemsLabel + " · " + statusBody(next));
        n.setOrderId(order.getOrderId());
        NotificationRepository.getInstance().send(order.getUserId(), n);
    }

    private static String buttonLabelFor(Order.Status next) {
        switch (next) {
            case PACKED:     return "Mark as Packed";
            case ON_THE_WAY: return "Mark as On the way";
            case DELIVERED:  return "Mark as Delivered";
            default:         return "";
        }
    }

    private static String statusTitle(Order.Status s) {
        switch (s) {
            case PACKED:     return "Packed";
            case ON_THE_WAY: return "On the way";
            case DELIVERED:  return "Delivered";
            default:         return "Order updated";
        }
    }

    private static String statusBody(Order.Status s) {
        switch (s) {
            case PACKED:     return "the seller packed it.";
            case ON_THE_WAY: return "is on the way to you.";
            case DELIVERED:  return "has been delivered.";
            default:         return "status updated.";
        }
    }

    private static String buildStatusText(Order.Status s) {
        switch (s) {
            case CONFIRMED:  return "Confirmed — ready to pack.";
            case PACKED:     return "Packed — ready to ship.";
            case ON_THE_WAY: return "On the way to the buyer.";
            case DELIVERED:  return "Delivered.";
            default:         return "";
        }
    }

    private static void paintProgress(Context ctx, Order.Status status,
                                      View seg1, View seg2, View seg3, View seg4) {
        int active = (status == null ? Order.Status.CONFIRMED : status).ordinal() + 1;
        int activeColor   = ContextCompat.getColor(ctx, R.color.home_balance_background);
        int inactiveColor = ContextCompat.getColor(ctx, R.color.home_placeholder);
        View[] segs = {seg1, seg2, seg3, seg4};
        for (int i = 0; i < segs.length; i++) {
            ((GradientDrawable) segs[i].getBackground().mutate())
                    .setColor(i < active ? activeColor : inactiveColor);
        }
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
