package com.example.avoid;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.avoid.adapter.MasonryGapDecoration;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.Product;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.Comparator;
import java.util.List;

/**
 * Public store page. Black-bg header with the store identity sits above the banner; below the
 * banner, three product sections (Best Sellers, Recommendation, All Products) mirror the home
 * page exactly. When the store has no products yet, the sections are hidden and a "Stay tuned
 * for new arrivals" message appears between the banner and the bottom of the screen.
 */
public class StoreDetailsFragment extends Fragment {

    private static final String ARG_STORE = "store";

    private Store store;

    private MaterialButton followButton;

    public static StoreDetailsFragment newInstance(@NonNull Store store) {
        StoreDetailsFragment fragment = new StoreDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STORE, store);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_store_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { close(); return; }
        store = (Store) args.getSerializable(ARG_STORE);
        if (store == null) { close(); return; }

        ImageButton back = view.findViewById(R.id.storeDetailsBack);
        back.setOnClickListener(v -> close());

        bindHeader(view);
        bindBanner(view);
        loadProducts(view);
    }

    private void bindHeader(View root) {
        ShapeableImageView logo = root.findViewById(R.id.storeDetailsLogo);
        TextView name           = root.findViewById(R.id.storeDetailsName);
        TextView location       = root.findViewById(R.id.storeDetailsLocation);
        TextView description    = root.findViewById(R.id.storeDetailsDescription);
        followButton            = root.findViewById(R.id.storeDetailsFollowButton);

        name.setText(store.getName() != null ? store.getName() : "Store");
        location.setText(store.getLocation() != null ? store.getLocation() : "");
        if (store.getDescription() != null && !store.getDescription().trim().isEmpty()) {
            description.setVisibility(View.VISIBLE);
            description.setText(store.getDescription());
        } else {
            description.setVisibility(View.GONE);
        }

        if (store.getLogoUrl() != null && !store.getLogoUrl().isEmpty()) {
            Glide.with(this).load(store.getLogoUrl()).centerCrop().into(logo);
        } else {
            logo.setImageResource(R.drawable.ic_profile);
        }

        if (isOwnStore()) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setVisibility(View.VISIBLE);
            renderFollowState();
            followButton.setOnClickListener(v -> toggleFollow());
        }
    }

    private boolean isOwnStore() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null) return false;
        return store.getOwnerId() != null
                && store.getOwnerId().equals(session.getCurrentUser().getId());
    }

    private void bindBanner(View root) {
        ShapeableImageView banner = root.findViewById(R.id.storeDetailsBanner);
        if (store.getBannerUrl() != null && !store.getBannerUrl().isEmpty()) {
            Glide.with(this).load(store.getBannerUrl()).centerCrop().into(banner);
        }
    }

    private void loadProducts(View root) {
        View sections   = root.findViewById(R.id.storeDetailsSections);
        View emptyState = root.findViewById(R.id.storeDetailsEmpty);
        RecyclerView bestSellers      = root.findViewById(R.id.storeBestSellersRecyclerView);
        RecyclerView recommendations  = root.findViewById(R.id.storeRecommendationsRecyclerView);
        RecyclerView allProducts      = root.findViewById(R.id.storeAllProductsRecyclerView);

        bestSellers.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendations.setLayoutManager(new LinearLayoutManager(requireContext()));
        allProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        allProducts.addItemDecoration(new MasonryGapDecoration(
                (int) (8 * getResources().getDisplayMetrics().density)));

        // Skeletons while loading.
        sections.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        bestSellers.setAdapter(new SkeletonAdapter(R.layout.item_product_card_skeleton, 4));
        recommendations.setAdapter(new SkeletonAdapter(R.layout.item_product_list_skeleton, 3));
        allProducts.setAdapter(new SkeletonAdapter(R.layout.item_product_card_skeleton, 6));

        if (store.getOwnerId() == null) {
            sections.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        ProductRepository.getInstance().loadProductsForStore(store.getOwnerId(),
                new ProductRepository.Callback<List<Product>>() {
                    @Override public void onSuccess(List<Product> products) {
                        if (!isAdded()) return;
                        if (products.isEmpty()) {
                            sections.setVisibility(View.GONE);
                            emptyState.setVisibility(View.VISIBLE);
                            return;
                        }
                        sections.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);

                        // Best sellers = top 8 by itemsSold.
                        List<Product> top = sortBy(products, (a, b) ->
                                Long.compare(b.getItemsSold(), a.getItemsSold()));
                        bestSellers.setAdapter(new ProductAdapter(
                                top.subList(0, Math.min(8, top.size())),
                                ProductAdapter.LayoutMode.CARD,
                                StoreDetailsFragment.this::openProduct));

                        // Recommendations = newest first, capped at 8.
                        List<Product> newest = sortBy(products, (a, b) ->
                                Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        recommendations.setAdapter(new ProductAdapter(
                                newest.subList(0, Math.min(8, newest.size())),
                                ProductAdapter.LayoutMode.LIST,
                                StoreDetailsFragment.this::openProduct));

                        // All Products in masonry-style grid (uniform card heights).
                        allProducts.setAdapter(new ProductAdapter(
                                newest, ProductAdapter.LayoutMode.GRID,
                                StoreDetailsFragment.this::openProduct));
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        if (!isAdded()) return;
                        sections.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private static List<Product> sortBy(List<Product> products, Comparator<Product> cmp) {
        List<Product> copy = new java.util.ArrayList<>(products);
        java.util.Collections.sort(copy, cmp);
        return copy;
    }

    private void openProduct(Product product) {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }

    private void toggleFollow() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
            return;
        }
        User user = session.getCurrentUser();
        if (user == null || store.getOwnerId() == null) return;
        boolean target = !user.isFollowing(store.getOwnerId());
        UserRepository.getInstance().setFollowing(user, store.getOwnerId(), target, null);
        renderFollowState();
    }

    private void renderFollowState() {
        if (followButton == null) return;
        UserSession session = UserSession.getInstance();
        boolean following = session.isLoggedIn()
                && session.getCurrentUser() != null
                && session.getCurrentUser().isFollowing(store.getOwnerId());
        if (following) {
            followButton.setText("Following");
            followButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.home_balance_background)));
            followButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.home_white));
        } else {
            followButton.setText("Follow");
            followButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.white)));
            followButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
    }

    private void close() {
        if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
    }
}
