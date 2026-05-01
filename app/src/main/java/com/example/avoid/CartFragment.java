package com.example.avoid;

import android.content.Intent;
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

import com.example.avoid.adapter.CartAdapter;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment {

    private RecyclerView cartItemsRecyclerView;
    private RecyclerView lastSeenRecyclerView;
    private TextView cartTotalPrice;
    private View cartScrollView;
    private View cartBottomBar;
    private View cartEmptyContainer;
    private List<CartProduct> cartItems;

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

        view.findViewById(R.id.btnCheckout).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CheckoutActivity.class)));

        view.findViewById(R.id.cartExploreButton).setOnClickListener(v -> openExplore());

        setupCartItems();
        setupLastSeen();
        updateTotal();
        renderEmptyState();
    }

    private final Runnable sessionListener = () -> {
        if (cartItemsRecyclerView != null) {
            setupCartItems();
            updateTotal();
            renderEmptyState();
        }
    };

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
        if (cartItemsRecyclerView != null) {
            setupCartItems();
            updateTotal();
            renderEmptyState();
        }
    }

    private void renderEmptyState() {
        boolean empty = cartItems == null || cartItems.isEmpty();
        cartScrollView.setVisibility(empty ? View.GONE : View.VISIBLE);
        cartBottomBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        cartEmptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openExplore() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ExploreProductsFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void setupCartItems() {
        cartItems = UserSession.getInstance().getCurrentUser().getCart().getItems();

        CartAdapter cartAdapter = new CartAdapter(cartItems, this::updateTotal);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartItemsRecyclerView.setAdapter(cartAdapter);
        cartItemsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupLastSeen() {
        List<Product> lastSeen = new ArrayList<>();
        lastSeen.add(new Product("Redmi 9A",    "$ 348",    "South Jakarta",   "4.8 | Sold 250+"));
        lastSeen.add(new Product("Redmi 11T",   "$ 508.42", "South Jakarta",   "4.8 | Sold 250+"));
        lastSeen.add(new Product("iPhone 13",   "$ 999",    "Central Jakarta", "4.9 | Sold 300+"));
        lastSeen.add(new Product("Dell XPS 13", "$ 1325",   "West Jakarta",    "4.7 | Sold 190+"));

        lastSeenRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        lastSeenRecyclerView.setAdapter(
                new ProductAdapter(lastSeen, ProductAdapter.LayoutMode.CARD, this::openProductDetails));
        lastSeenRecyclerView.setNestedScrollingEnabled(false);
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }

    private void updateTotal() {
        double total = 0;
        int count = 0;
        for (CartProduct item : cartItems) {
            total += item.getPriceValue() * item.getQuantity();
            count += item.getQuantity();
        }
        cartTotalPrice.setText(String.format(Locale.US, "$ %,.2f", total));
        renderEmptyState();
        UserRepository.getInstance().saveCartForCurrentUser();

        if (getActivity() instanceof CartBadgeUpdater) {
            ((CartBadgeUpdater) getActivity()).updateCartBadge(count);
        }
    }
}
