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
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView cartItemsRecyclerView;
    private TextView cartTotalPrice;
    private List<CartProduct> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);

        initializeViews();
        setupSystemBars();
        applyWindowInsets();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        setupCartItems();
        updateTotal();
    }

    private void initializeViews() {
        cartItemsRecyclerView = findViewById(R.id.checkoutItemsRecyclerView);
        cartTotalPrice = findViewById(R.id.checkoutTotalPrice);
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
        cartItems = CartManager.getInstance().getCartItems();

        CartAdapter cartAdapter = new CartAdapter(cartItems, this::updateTotal);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartAdapter);
        cartItemsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void updateTotal() {
        double total = 0;
        for (CartProduct item : cartItems) {
            total += item.getPriceValue() * item.getQuantity();
        }
        cartTotalPrice.setText(String.format(Locale.US, "$ %,.2f", total));
    }
}