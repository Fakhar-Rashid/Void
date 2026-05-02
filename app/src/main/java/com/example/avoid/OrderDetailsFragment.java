package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.OrderLineAdapter;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Review;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Buyer's order details — opened on top of MainActivity's fragment_container so navigating
 * to product details below uses the same single overlay stack the rest of the app already
 * relies on (Home → details, Cart → details, etc.).
 */
public class OrderDetailsFragment extends Fragment {

    private static final String ARG_ORDER = "order";

    private Order order;
    private OrderLineAdapter adapter;
    private RecyclerView itemsRecycler;

    /** storeId → display name. Populated as store docs come back. */
    private final Map<String, String> storeNamesById = new HashMap<>();
    /** productId → review. Populated by {@link #loadReviews}. */
    private final Map<String, Review> reviewsByProductId = new HashMap<>();

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

        bindSummary(view);

        itemsRecycler = view.findViewById(R.id.odItemsRecyclerView);
        itemsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderLineAdapter(order.getItems(), OrderLineAdapter.Mode.BUYER,
                this::handleItemAction);
        adapter.setItemClickListener((item, position) -> openProductDetails(item.getProductId()));
        itemsRecycler.setAdapter(adapter);

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
        int stores = order.getStoreIds().size();
        String summary = items + " item" + (items == 1 ? "" : "s");
        if (stores > 1) summary += " · " + stores + " stores";
        ((TextView) root.findViewById(R.id.odSummaryItems)).setText(summary);

        ((TextView) root.findViewById(R.id.odSummaryTotal))
                .setText(String.format(Locale.US, "$%,.2f", order.getTotalAmount()));
    }

    private void loadStoreName(String storeId) {
        if (storeId == null) return;
        UserRepository.getInstance().loadStore(storeId,
                new UserRepository.Callback<com.example.avoid.model.Store>() {
                    @Override
                    public void onSuccess(com.example.avoid.model.Store store) {
                        if (!isAdded() || store == null) return;
                        storeNamesById.put(storeId, store.getName());
                        applyStoreNamesToItems();
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void applyStoreNamesToItems() {
        if (itemsRecycler == null) return;
        for (int i = 0; i < itemsRecycler.getChildCount(); i++) {
            View row = itemsRecycler.getChildAt(i);
            int pos = itemsRecycler.getChildAdapterPosition(row);
            if (pos == RecyclerView.NO_POSITION || pos >= order.getItems().size()) continue;
            OrderLineItem item = order.getItems().get(pos);
            String name = storeNamesById.get(item.getStoreId());
            TextView storeText = row.findViewById(R.id.orderLineStore);
            if (storeText == null) continue;
            if (name != null && !name.isEmpty()) {
                storeText.setVisibility(View.VISIBLE);
                storeText.setText("Sold by " + name);
            }
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
                        if (adapter != null) adapter.setReviews(reviewsByProductId);
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void handleItemAction(OrderLineItem item, int position) {
        ReviewOrderBottomSheet.showForItem(getParentFragmentManager(), order, item, false,
                () -> {
                    if (adapter != null) adapter.notifyItemChanged(position);
                    loadReviews();
                });
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
