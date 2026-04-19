package com.example.avoid;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView bestSellersRecyclerView;
    private RecyclerView recommendationsRecyclerView;

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
        setupProductSections();
    }

    private void setupProductSections() {
        bestSellersRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        bestSellersRecyclerView.setAdapter(
                new ProductAdapter(createBestSellerProducts(), ProductAdapter.LayoutMode.CARD, this::openProductDetails));

        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recommendationsRecyclerView.setAdapter(
                new ProductAdapter(createRecommendationProducts(), ProductAdapter.LayoutMode.LIST, this::openProductDetails));
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }

    private List<Product> createBestSellerProducts() {
        ArrayList<Product> products = new ArrayList<>();
        products.add(new Product("Xiamo 11T",          "$ 407.70", "North Jakarta",   "4.8 | Sold 250+"));
        products.add(new Product("Macbook Pro M1",      "$ 1203",   "South Jakarta",   "4.8 | Sold 250+"));
        products.add(new Product("Redmi Note 10",       "$ 392.10", "South Jakarta",   "4.8 | Sold 250+"));
        products.add(new Product("Asus ROG Zephyrus",   "$ 1480",   "West Jakarta",    "4.9 | Sold 180+"));
        products.add(new Product("Samsung Odyssey G5",  "$ 610",    "Central Jakarta", "4.7 | Sold 120+"));
        return products;
    }

    private List<Product> createRecommendationProducts() {
        ArrayList<Product> products = new ArrayList<>();
        products.add(new Product("Redmi 11T",     "$ 508.42", "South Jakarta",   "4.8 | Sold 250+"));
        products.add(new Product("Redmi 9A",      "$ 348",    "South Jakarta",   "4.8 | Sold 250+"));
        products.add(new Product("Acer Swift 3",  "$ 730",    "North Jakarta",   "4.6 | Sold 140+"));
        products.add(new Product("iPhone 13",     "$ 999",    "Central Jakarta", "4.9 | Sold 300+"));
        products.add(new Product("Dell XPS 13",   "$ 1325",   "West Jakarta",    "4.7 | Sold 190+"));
        products.add(new Product("Lenovo Legion 5","$ 1189",  "East Jakarta",    "4.8 | Sold 160+"));
        return products;
    }
}
