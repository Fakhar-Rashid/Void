package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.CartAdapter;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.CartItem;
import com.example.avoid.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CartFragment extends Fragment {

    private static final String TAG = "CartFragment";

    private RecyclerView cartItemsRecyclerView;
    private RecyclerView lastSeenRecyclerView;
    private TextView cartTotalPrice;
    private View cartScrollView;
    private View cartBottomBar;
    private View cartEmptyContainer;

    private List<CartItem> cartItems = new ArrayList<>();
    private final Map<String, Product> productsById = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cartItemsRecyclerView = view.findViewById(R.id.cartItemsRecyclerView);
        lastSeenRecyclerView  = view.findViewById(R.id.lastSeenRecyclerView);
        cartTotalPrice        = view.findViewById(R.id.cartTotalPrice);
        cartScrollView        = view.findViewById(R.id.cartScrollView);
        cartBottomBar         = view.findViewById(R.id.cartBottomBar);
        cartEmptyContainer    = view.findViewById(R.id.cartEmptyContainer);

        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartItemsRecyclerView.setNestedScrollingEnabled(false);

        view.findViewById(R.id.btnCheckout).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CheckoutActivity.class)));

        view.findViewById(R.id.cartExploreButton).setOnClickListener(v -> openExplore());

        loadCart();
        setupLastSeen();
    }

    private final Runnable sessionListener = this::loadCart;

    @Override
    public void onStart() {
        super.onStart();
        UserSession.getInstance().addListener(sessionListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        UserSession.getInstance().removeListener(sessionListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        if (cartItemsRecyclerView == null) return;

        cartItems = UserSession.getInstance().getCurrentUser().getCart().getItems();

        Set<String> ids = new LinkedHashSet<>();
        for (CartItem item : cartItems) {
            if (item.getProductId() != null) ids.add(item.getProductId());
        }

        if (ids.isEmpty()) {
            productsById.clear();
            cartItemsRecyclerView.setAdapter(buildCartAdapter());
            updateTotalAndEmpty();
            return;
        }

        cartItemsRecyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_list_skeleton, Math.min(ids.size(), 4)));

        ProductRepository.getInstance().loadProductsByIds(new ArrayList<>(ids),
                new ProductRepository.Callback<List<Product>>() {
                    @Override public void onSuccess(List<Product> products) {
                        productsById.clear();
                        for (Product p : products) productsById.put(p.getId(), p);
                        cartItemsRecyclerView.setAdapter(buildCartAdapter());
                        updateTotalAndEmpty();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load cart products", e);
                        cartItemsRecyclerView.setAdapter(buildCartAdapter());
                        updateTotalAndEmpty();
                    }
                });
    }

    private void onCartChanged() {
        UserRepository.getInstance().saveCartForCurrentUser();
        updateTotalAndEmpty();
    }

    private void updateTotalAndEmpty() {
        double total = 0;
        int count = 0;
        for (CartItem item : cartItems) {
            Product p = productsById.get(item.getProductId());
            if (p != null) total += p.getPrice() * item.getQuantity();
            count += item.getQuantity();
        }
        cartTotalPrice.setText(String.format(Locale.US, "$ %,.2f", total));
        renderEmptyState();

        if (getActivity() instanceof CartBadgeUpdater) {
            ((CartBadgeUpdater) getActivity()).updateCartBadge(count);
        }
    }

    private void renderEmptyState() {
        boolean empty = cartItems == null || cartItems.isEmpty();
        cartScrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
        cartBottomBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        cartEmptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private CartAdapter buildCartAdapter() {
        CartAdapter adapter = new CartAdapter(cartItems, productsById, this::onCartChanged);
        adapter.setOnItemClickListener((cartItem, product) -> openProductDetails(product));
        return adapter;
    }

    private void openExplore() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ExploreProductsFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void setupLastSeen() {
        ProductRepository.getInstance().loadTopProducts(8, new ProductRepository.Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> products) {
                lastSeenRecyclerView.setLayoutManager(
                        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                lastSeenRecyclerView.setAdapter(new ProductAdapter(products,
                        ProductAdapter.LayoutMode.CARD, CartFragment.this::openProductDetails));
                lastSeenRecyclerView.setNestedScrollingEnabled(false);
            }
            @Override public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Failed to load last seen", e);
            }
        });
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }
}
