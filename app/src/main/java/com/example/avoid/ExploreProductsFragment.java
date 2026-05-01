package com.example.avoid;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ExploreProductsFragment extends Fragment {

    private static final String TAG = "ExploreProductsFragment";
    private static final String COLLECTION = "products";
    private static final String ARG_INITIAL_QUERY = "initial_query";

    private static final List<String> TRENDING_SEARCH_TERMS = Arrays.asList(
            "Asus ROG", "Asus Zenbook", "Macbook Air M2",
            "MSI Creator", "Macbook Pro", "Optix MEG381CQ"
    );

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Product> allProducts = new ArrayList<>();

    private EditText searchInput;
    private RecyclerView resultsRecyclerView;
    private TextView trendingProductsTitle;
    private TextView emptyState;
    private ProductAdapter resultsAdapter;

    public static ExploreProductsFragment newInstance(@Nullable String initialQuery) {
        ExploreProductsFragment fragment = new ExploreProductsFragment();
        if (initialQuery != null) {
            Bundle args = new Bundle();
            args.putString(ARG_INITIAL_QUERY, initialQuery);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.exploreBackButton);
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        searchInput           = view.findViewById(R.id.exploreSearchInput);
        resultsRecyclerView   = view.findViewById(R.id.exploreResultsRecyclerView);
        trendingProductsTitle = view.findViewById(R.id.trendingProductsTitle);
        emptyState            = view.findViewById(R.id.exploreEmptyState);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_list_skeleton, 5));

        wireCategoryChips(view);
        wireTrendingSearchChips(view.findViewById(R.id.trendingSearchGroup));
        wireSearchInput();

        loadAllProducts();

        if (getArguments() != null) {
            String initial = getArguments().getString(ARG_INITIAL_QUERY);
            if (initial != null && !initial.isEmpty()) {
                searchInput.setText(initial);
                searchInput.setSelection(initial.length());
            }
        }

        focusSearchInput();
    }

    private void focusSearchInput() {
        searchInput.requestFocus();
        searchInput.post(() -> {
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void wireCategoryChips(View root) {
        int[] chipIds = {
                R.id.categoryChipLaptop,
                R.id.categoryChipSmartphone,
                R.id.categoryChipMonitor,
                R.id.categoryChipAccessories
        };
        String[] labels = {
                getString(R.string.home_category_laptop),
                getString(R.string.home_category_smartphone),
                getString(R.string.home_category_monitor),
                getString(R.string.home_category_accessories)
        };
        for (int i = 0; i < chipIds.length; i++) {
            View chip = root.findViewById(chipIds[i]);
            if (chip == null) continue;
            final String label = labels[i];
            chip.setOnClickListener(v -> {
                searchInput.setText(label);
                searchInput.setSelection(label.length());
            });
        }
    }

    private void wireTrendingSearchChips(ChipGroup group) {
        group.removeAllViews();
        ColorStateList chipBg = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.explore_trending_chip_bg));
        ColorStateList chipText = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.explore_trending_chip_text));
        float pillRadius = getResources().getDisplayMetrics().density * 100f;

        for (String term : TRENDING_SEARCH_TERMS) {
            Chip chip = new Chip(requireContext());
            chip.setText(term);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setChipBackgroundColor(chipBg);
            chip.setTextColor(chipText);
            chip.setChipStrokeWidth(0f);
            chip.setChipCornerRadius(pillRadius);
            chip.setOnClickListener(v -> {
                searchInput.setText(term);
                searchInput.setSelection(term.length());
            });
            group.addView(chip);
        }
    }

    private void wireSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                applyFilter(s.toString());
            }
        });
    }

    private void loadAllProducts() {
        db.collection(COLLECTION).get()
                .addOnSuccessListener(snapshot -> {
                    allProducts.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        allProducts.add(p);
                    }
                    applyFilter(searchInput.getText().toString());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load products", e));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Product> filtered;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(allProducts);
            trendingProductsTitle.setText(R.string.explore_trending_products);
        } else {
            filtered = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(p);
                }
            }
            trendingProductsTitle.setText(R.string.explore_search_results);
        }

        emptyState.setVisibility(filtered.isEmpty() && !allProducts.isEmpty() ? View.VISIBLE : View.GONE);

        resultsAdapter = new ProductAdapter(filtered, ProductAdapter.LayoutMode.LIST, this::openProductDetails);
        resultsRecyclerView.setAdapter(resultsAdapter);
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }
}
