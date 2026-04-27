package com.example.avoid;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String COLLECTION = "products";

    private RecyclerView bestSellersRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bestSellersRecyclerView    = view.findViewById(R.id.bestSellersRecyclerView);
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView);

        bestSellersRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.searchBar).setOnClickListener(v -> openExplore(null));
        wireCategoryChips(view);

        loadProducts(Product.CATEGORY_BEST_SELLER, ProductAdapter.LayoutMode.CARD, bestSellersRecyclerView);
        loadProducts(Product.CATEGORY_RECOMMENDATION, ProductAdapter.LayoutMode.LIST, recommendationsRecyclerView);
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
            chip.setOnClickListener(v -> openExplore(label));
        }
    }

    private void openExplore(@Nullable String initialQuery) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ExploreProductsFragment.newInstance(initialQuery))
                .addToBackStack(null)
                .commit();
    }

    private void loadProducts(String category, ProductAdapter.LayoutMode mode, RecyclerView target) {
        db.collection(COLLECTION)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        products.add(p);
                    }
                    target.setAdapter(new ProductAdapter(products, mode, this::openProductDetails));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load " + category, e));
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }
}
