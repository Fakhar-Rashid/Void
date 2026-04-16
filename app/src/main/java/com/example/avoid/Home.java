package com.example.avoid;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.model.Product;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class Home extends AppCompatActivity {

    private RecyclerView bestSellersRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupSystemBars();
        applyWindowInsets();
        setupBottomNavigation();
        setupProductSections();
    }

    private void initializeViews() {
        bestSellersRecyclerView = findViewById(R.id.bestSellersRecyclerView);
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(R.id.main));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    0
            );
            return insets;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setItemActiveIndicatorEnabled(false);
    }

    private void setupProductSections() {
        List<Product> bestSellerProducts = createBestSellerProducts();
        List<Product> recommendationProducts = createRecommendationProducts();

        bestSellersRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        bestSellersRecyclerView.setAdapter(
                new ProductAdapter(bestSellerProducts, ProductAdapter.LayoutMode.CARD)
        );

        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recommendationsRecyclerView.setAdapter(
                new ProductAdapter(recommendationProducts, ProductAdapter.LayoutMode.LIST)
        );
    }

    private List<Product> createBestSellerProducts() {
        ArrayList<Product> products = new ArrayList<>();
        products.add(new Product("Xiamo 11T", "$ 407.70", "North Jakarta", "4.8 | Sold 250+"));
        products.add(new Product("Macbook Pro M1", "$ 1203", "South Jakarta", "4.8 | Sold 250+"));
        products.add(new Product("Redmi Note 10", "$ 392.10", "South Jakarta", "4.8 | Sold 250+"));
        products.add(new Product("Asus ROG Zephyrus", "$ 1480", "West Jakarta", "4.9 | Sold 180+"));
        products.add(new Product("Samsung Odyssey G5", "$ 610", "Central Jakarta", "4.7 | Sold 120+"));
        return products;
    }

    private List<Product> createRecommendationProducts() {
        ArrayList<Product> products = new ArrayList<>();
        products.add(new Product("Redmi 11T", "$ 508.42", "South Jakarta", "4.8 | Sold 250+"));
        products.add(new Product("Redmi 9A", "$ 348", "South Jakarta", "4.8 | Sold 250+"));
        products.add(new Product("Acer Swift 3", "$ 730", "North Jakarta", "4.6 | Sold 140+"));
        products.add(new Product("iPhone 13", "$ 999", "Central Jakarta", "4.9 | Sold 300+"));
        products.add(new Product("Dell XPS 13", "$ 1325", "West Jakarta", "4.7 | Sold 190+"));
        products.add(new Product("Lenovo Legion 5", "$ 1189", "East Jakarta", "4.8 | Sold 160+"));
        return products;
    }
}
