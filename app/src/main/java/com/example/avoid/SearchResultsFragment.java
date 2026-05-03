package com.example.avoid;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.avoid.adapter.MasonryGapDecoration;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable browse / search-results screen.
 *
 * <ul>
 *   <li>Opens via {@link #newInstance(String, String)} — pass a non-null query for
 *       "Search results for "X"" mode, or just a custom title for a "See all" listing.</li>
 *   <li>Renders products in a 2-column staggered (masonry) grid using the existing
 *       {@code item_product_card} via {@link ProductAdapter} {@code LayoutMode.CARD}.</li>
 *   <li>Loads every product from Firestore once, then filters client-side by name when a
 *       query is set. Matches the existing {@code ExploreProductsFragment} substring style.</li>
 * </ul>
 */
public class SearchResultsFragment extends Fragment {

    private static final String TAG = "SearchResultsFragment";
    private static final String COLLECTION = "products";
    private static final String ARG_QUERY = "query";
    private static final String ARG_TITLE = "title";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Product> allProducts = new ArrayList<>();
    private RecyclerView recyclerView;
    private View emptyState;
    private TextView emptyHint;
    @Nullable private String query;
    @Nullable private String customTitle;

    public static SearchResultsFragment newInstance(@Nullable String query, @Nullable String title) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        if (query != null) args.putString(ARG_QUERY, query);
        if (title != null) args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        query       = args != null ? args.getString(ARG_QUERY) : null;
        customTitle = args != null ? args.getString(ARG_TITLE) : null;

        ImageButton back = view.findViewById(R.id.searchResultsBack);
        back.setOnClickListener(v -> {
            if (isAdded()) requireActivity().getSupportFragmentManager().popBackStack();
        });

        TextView title = view.findViewById(R.id.searchResultsTitle);
        title.setText(buildTitle());

        recyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        emptyState   = view.findViewById(R.id.searchResultsEmpty);
        emptyHint    = view.findViewById(R.id.searchResultsEmptyHint);

        StaggeredGridLayoutManager glm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        // Keeps cards stable when one column finishes loading first.
        glm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(glm);
        recyclerView.addItemDecoration(new MasonryGapDecoration(
                (int) (8 * getResources().getDisplayMetrics().density)));

        recyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_card_skeleton, 6));

        loadProducts();
    }

    private String buildTitle() {
        if (query != null && !query.trim().isEmpty()) {
            return getString(R.string.search_results_title_query, query.trim());
        }
        if (customTitle != null && !customTitle.isEmpty()) return customTitle;
        return getString(R.string.search_results_title_all);
    }

    private void loadProducts() {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    allProducts.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        allProducts.add(p);
                    }
                    bind();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load products", e);
                    if (isAdded()) bind();
                });
    }

    private void bind() {
        List<Product> filtered;
        if (query == null || query.trim().isEmpty()) {
            filtered = new ArrayList<>(allProducts);
        } else {
            String q = query.trim().toLowerCase(Locale.ROOT);
            filtered = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(p);
                }
            }
        }

        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyHint.setText(query != null && !query.trim().isEmpty()
                    ? "Try a different search term."
                    : "No products are listed yet.");
            return;
        }
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new ProductAdapter(filtered, ProductAdapter.LayoutMode.GRID,
                this::openProductDetails));
    }

    private void openProductDetails(Product product) {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }
}
