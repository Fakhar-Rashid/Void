package com.example.avoid.seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.AddProductActivity;
import com.example.avoid.ProductRepository;
import com.example.avoid.R;
import com.example.avoid.UserRepository;
import com.example.avoid.UserSession;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Product;
import com.example.avoid.model.User;

import java.util.List;
import java.util.Locale;

public class SellerHomeFragment extends Fragment {

    private static final String TAG = "SellerHomeFragment";

    private RecyclerView recyclerView;
    private View emptyContainer;
    private TextView subtitleText;
    private TextView statEarnings, statSold, statOpen;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView   = view.findViewById(R.id.sellerProductsRecyclerView);
        emptyContainer = view.findViewById(R.id.sellerHomeEmptyContainer);
        subtitleText   = view.findViewById(R.id.sellerHomeSubtitle);
        statEarnings   = view.findViewById(R.id.sellerStatEarnings);
        statSold       = view.findViewById(R.id.sellerStatSold);
        statOpen       = view.findViewById(R.id.sellerStatOpen);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_list_skeleton, 4));

        loadMyProducts();
        loadStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyProducts();
        loadStats();
    }

    /**
     * Stats are derived from this seller's orders, not the user wallet:
     *   Earnings = sum of price × qty for line items delivered to the buyer.
     *   Sold     = count of items delivered.
     *   Open     = count of orders that still have a non-delivered item from this store.
     */
    private void loadStats() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) return;
        final String storeId = user.getId();

        UserRepository.getInstance().loadStoreOrders(storeId, orders -> {
            if (!isAdded()) return;
            double earnings = 0;
            int sold = 0;
            int open = 0;
            for (Order order : orders) {
                boolean hasOpen = false;
                for (OrderLineItem item : order.getItems()) {
                    if (!storeId.equals(item.getStoreId())) continue;
                    if (item.getStatus() == Order.Status.DELIVERED) {
                        sold += item.getQuantity();
                        earnings += item.getProductPrice() * item.getQuantity();
                    } else {
                        hasOpen = true;
                    }
                }
                if (hasOpen) open++;
            }
            statEarnings.setText(String.format(Locale.US, "$%,.0f", earnings));
            statSold.setText(String.valueOf(sold));
            statOpen.setText(String.valueOf(open));
        });
    }

    private void loadMyProducts() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) return;

        ProductRepository.getInstance().loadProductsForStore(user.getId(),
                new ProductRepository.Callback<List<Product>>() {
                    @Override public void onSuccess(List<Product> products) {
                        bindProducts(products);
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load seller's products", e);
                        bindProducts(new java.util.ArrayList<>());
                    }
                });
    }

    private void bindProducts(List<Product> products) {
        if (getView() == null) return;

        if (products.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
            subtitleText.setText("");
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        subtitleText.setText(String.format(Locale.US,
                "%d listing%s", products.size(), products.size() == 1 ? "" : "s"));

        ProductAdapter adapter = new ProductAdapter(products, ProductAdapter.LayoutMode.LIST,
                this::openEdit);
        recyclerView.setAdapter(adapter);

        // Long-press to delete.
        recyclerView.post(() -> wireLongPressForDelete(products));
    }

    private void wireLongPressForDelete(List<Product> products) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            int finalI = i;
            View child = recyclerView.getChildAt(i);
            child.setOnLongClickListener(v -> {
                if (finalI < products.size()) confirmDelete(products.get(finalI));
                return true;
            });
        }
    }

    private void openEdit(Product product) {
        if (product.getId() == null) return;
        startActivity(AddProductActivity.createIntent(requireContext(), product.getId()));
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(requireContext())
                .setTitle(product.getName())
                .setMessage(R.string.seller_product_delete_confirm)
                .setPositiveButton(R.string.seller_product_action_delete, (d, w) -> doDelete(product))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doDelete(Product product) {
        if (product.getId() == null) return;
        ProductRepository.getInstance().deleteProduct(product.getId(),
                new ProductRepository.Callback<Void>() {
                    @Override public void onSuccess(Void unused) {
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        loadMyProducts();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(),
                                e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Delete failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
