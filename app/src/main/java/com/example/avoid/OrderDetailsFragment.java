package com.example.avoid;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.OrderLineAdapter;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Buyer's order details. Items are grouped into one section per store ("shipment"), with a
 * single per-shipment progress strip — matching how parcels actually move (a store ships its
 * own items together, not piece by piece).
 */
public class OrderDetailsFragment extends Fragment {

    private static final String ARG_ORDER = "order";

    private Order order;
    private LinearLayout shipmentsContainer;

    /** storeId → display name. Populated as store docs come back. */
    private final Map<String, String> storeNamesById = new HashMap<>();
    /** productId → review. */
    private final Map<String, Review> reviewsByProductId = new HashMap<>();
    /** Per-shipment OrderLineAdapter so we can refresh review state without rebuilding the world. */
    private final Map<String, OrderLineAdapter> adaptersByStoreId = new HashMap<>();

    public static OrderDetailsFragment newInstance(@NonNull Order order) {
        OrderDetailsFragment fragment = new OrderDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { close(); return; }
        order = (Order) args.getSerializable(ARG_ORDER);
        if (order == null) { close(); return; }

        ImageButton back = view.findViewById(R.id.orderDetailsBack);
        back.setOnClickListener(v -> close());

        shipmentsContainer = view.findViewById(R.id.odShipmentsContainer);

        bindSummary(view);
        renderShipments();

        for (String storeId : order.getStoreIds()) loadStoreName(storeId);
        loadReviews();
    }

    private void bindSummary(View root) {
        String idShort = order.getOrderId() != null && order.getOrderId().length() > 8
                ? order.getOrderId().substring(0, 8).toUpperCase()
                : (order.getOrderId() != null ? order.getOrderId() : "—");
        ((TextView) root.findViewById(R.id.odSummaryId)).setText("#" + idShort);
        ((TextView) root.findViewById(R.id.odSummaryDate)).setText(order.getOrderDate() != null
                ? order.getOrderDate() : "—");

        int items = order.getTotalItemCount();
        ((TextView) root.findViewById(R.id.odSummaryItems))
                .setText(items + " item" + (items == 1 ? "" : "s"));

        int shipments = order.getStoreIds().size();
        TextView shipmentsLine = root.findViewById(R.id.odSummaryShipments);
        if (shipments > 1) {
            shipmentsLine.setVisibility(View.VISIBLE);
            shipmentsLine.setText("Arriving in " + shipments + " shipments");
        } else {
            shipmentsLine.setVisibility(View.GONE);
        }

        ((TextView) root.findViewById(R.id.odSummaryTotal))
                .setText(String.format(Locale.US, "$%,.2f", order.getTotalAmount()));
    }

    /** Build one shipment section per store, preserving insertion order. */
    private void renderShipments() {
        shipmentsContainer.removeAllViews();
        adaptersByStoreId.clear();

        Map<String, List<OrderLineItem>> byStore = new LinkedHashMap<>();
        for (OrderLineItem item : order.getItems()) {
            String key = item.getStoreId() != null ? item.getStoreId() : "";
            List<OrderLineItem> list = byStore.get(key);
            if (list == null) { list = new ArrayList<>(); byStore.put(key, list); }
            list.add(item);
        }

        int total = byStore.size();
        int index = 0;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Map.Entry<String, List<OrderLineItem>> entry : byStore.entrySet()) {
            index++;
            String storeId = entry.getKey();
            List<OrderLineItem> items = entry.getValue();
            View section = inflater.inflate(R.layout.view_shipment_section, shipmentsContainer, false);

            TextView label  = section.findViewById(R.id.shipmentLabel);
            TextView status = section.findViewById(R.id.shipmentStatusText);
            View seg1 = section.findViewById(R.id.shipmentSeg1);
            View seg2 = section.findViewById(R.id.shipmentSeg2);
            View seg3 = section.findViewById(R.id.shipmentSeg3);
            View seg4 = section.findViewById(R.id.shipmentSeg4);
            section.setTag(R.id.orderDetailsBack, storeId); // store the storeId for later store-name updates

            String storeName = storeNamesById.get(storeId);
            label.setText(buildLabel(index, total, storeName));

            Order.Status shipmentStatus = shipmentStatus(items);
            paintProgress(requireContext(), shipmentStatus, seg1, seg2, seg3, seg4);
            status.setText(shipmentStatusText(items, shipmentStatus));

            // Items list: one little RecyclerView per shipment so each adapter can manage its
            // own review map without bleed-over.
            LinearLayout itemsHost = section.findViewById(R.id.shipmentItemsContainer);
            RecyclerView rv = new RecyclerView(requireContext());
            rv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setNestedScrollingEnabled(false);
            OrderLineAdapter adapter = new OrderLineAdapter(items, OrderLineAdapter.Mode.BUYER,
                    this::handleItemAction);
            adapter.setItemClickListener((item, pos) -> openProductDetails(item.getProductId()));
            adapter.setReviews(reviewsByProductId);
            rv.setAdapter(adapter);
            itemsHost.addView(rv);

            adaptersByStoreId.put(storeId, adapter);
            shipmentsContainer.addView(section);
        }
    }

    private static Order.Status shipmentStatus(List<OrderLineItem> items) {
        // Per-store status = the lowest status of the shipment's items. Since the seller now
        // bumps every item in lockstep, all items in a group share the same status — but
        // computing min keeps the UI honest for legacy orders.
        Order.Status lowest = Order.Status.DELIVERED;
        for (OrderLineItem item : items) {
            Order.Status s = item.getStatus() != null ? item.getStatus() : Order.Status.CONFIRMED;
            if (s.ordinal() < lowest.ordinal()) lowest = s;
        }
        return lowest;
    }

    private static String shipmentStatusText(List<OrderLineItem> items, Order.Status status) {
        long ts = 0;
        for (OrderLineItem item : items) ts = Math.max(ts, item.getStatusTimestamp());
        String when = ts > 0
                ? DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                : "—";
        switch (status) {
            case CONFIRMED:  return "Confirmed " + when;
            case PACKED:     return "Packed " + when;
            case ON_THE_WAY: return "On the way " + when;
            case DELIVERED:  return "Delivered " + when;
            default:         return "";
        }
    }

    private static String buildLabel(int index, int total, @Nullable String storeName) {
        StringBuilder sb = new StringBuilder();
        if (total > 1) sb.append("SHIPMENT ").append(index);
        if (storeName != null && !storeName.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(storeName.toUpperCase(Locale.US));
        } else if (sb.length() == 0) {
            sb.append("SHIPMENT");
        }
        return sb.toString();
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

    private void loadStoreName(String storeId) {
        if (storeId == null) return;
        UserRepository.getInstance().loadStore(storeId,
                new UserRepository.Callback<com.example.avoid.model.Store>() {
                    @Override
                    public void onSuccess(com.example.avoid.model.Store store) {
                        if (!isAdded() || store == null) return;
                        storeNamesById.put(storeId, store.getName());
                        applyStoreNamesToHeaders();
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void applyStoreNamesToHeaders() {
        if (shipmentsContainer == null) return;
        int total = shipmentsContainer.getChildCount();
        for (int i = 0; i < total; i++) {
            View section = shipmentsContainer.getChildAt(i);
            Object tag = section.getTag(R.id.orderDetailsBack);
            if (!(tag instanceof String)) continue;
            String storeId = (String) tag;
            String storeName = storeNamesById.get(storeId);
            TextView label = section.findViewById(R.id.shipmentLabel);
            if (label != null) label.setText(buildLabel(i + 1, total, storeName));
        }
    }

    private void loadReviews() {
        if (order.getOrderId() == null) return;
        ReviewRepository.getInstance().loadReviewsByOrderId(order.getOrderId(),
                new ReviewRepository.Callback<List<Review>>() {
                    @Override
                    public void onSuccess(List<Review> result) {
                        if (!isAdded()) return;
                        reviewsByProductId.clear();
                        for (Review r : result) {
                            if (r.getProductId() != null) reviewsByProductId.put(r.getProductId(), r);
                        }
                        for (OrderLineAdapter adapter : adaptersByStoreId.values()) {
                            adapter.setReviews(reviewsByProductId);
                        }
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void handleItemAction(OrderLineItem item, int position) {
        ReviewOrderBottomSheet.showForItem(getParentFragmentManager(), order, item, false,
                this::loadReviews);
    }

    private void openProductDetails(String productId) {
        if (productId == null) return;
        ProductRepository.getInstance().loadProduct(productId,
                new ProductRepository.Callback<com.example.avoid.model.Product>() {
                    @Override
                    public void onSuccess(com.example.avoid.model.Product product) {
                        if (!isAdded() || product == null) return;
                        if (product.getId() == null) product.setId(productId);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                                .addToBackStack(null)
                                .commit();
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void close() {
        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
    }
}
