package com.example.avoid;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.CartAdapter;
import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private RecyclerView cartItemsRecyclerView;
    private RecyclerView lastSeenRecyclerView;
    private TextView cartTotalPrice;
    private List<CartProduct> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        initializeViews();
        setupSystemBars();
        applyWindowInsets();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupCartItems();
        setupLastSeen();
        updateTotal();
    }

    private void initializeViews() {
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        lastSeenRecyclerView = findViewById(R.id.lastSeenRecyclerView);
        cartTotalPrice = findViewById(R.id.cartTotalPrice);
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
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupCartItems() {
        cartItems = new ArrayList<>();
        cartItems.add(new CartProduct(
                new Product("Xiamo 11T", "$ 407.70", "North Jakarta", "4.8 | Sold 250+"), "White", 1));
        cartItems.add(new CartProduct(
                new Product("Redmi 9A", "$ 348", "South Jakarta", "4.8 | Sold 250+"), "Black", 1));
        cartItems.add(new CartProduct(
                new Product("Macbook Pro M1", "$ 1203", "South Jakarta", "4.8 | Sold 250+"), "Silver", 1));

        CartAdapter cartAdapter = new CartAdapter(cartItems, this::updateTotal);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartAdapter);
        cartItemsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupLastSeen() {
        List<Product> lastSeen = new ArrayList<>();
        lastSeen.add(new Product("Redmi 9A", "$ 348", "South Jakarta", "4.8 | Sold 250+"));
        lastSeen.add(new Product("Redmi 11T", "$ 508.42", "South Jakarta", "4.8 | Sold 250+"));
        lastSeen.add(new Product("iPhone 13", "$ 999", "Central Jakarta", "4.9 | Sold 300+"));
        lastSeen.add(new Product("Dell XPS 13", "$ 1325", "West Jakarta", "4.7 | Sold 190+"));

        lastSeenRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        lastSeenRecyclerView.setAdapter(
                new ProductAdapter(lastSeen, ProductAdapter.LayoutMode.LIST_HORIZONTAL));
        lastSeenRecyclerView.setNestedScrollingEnabled(false);
    }

    private void updateTotal() {
        double total = 0;
        for (CartProduct item : cartItems) {
            total += item.getPriceValue() * item.getQuantity();
        }
        cartTotalPrice.setText(String.format(Locale.US, "$ %,.2f", total));
    }
}
