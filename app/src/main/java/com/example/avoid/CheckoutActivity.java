package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

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
import com.example.avoid.model.Cart;
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Order;
import com.example.avoid.model.Product;
import com.example.avoid.model.User;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        configureCheckoutButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureCheckoutButton();
    }

    private void configureCheckoutButton() {
        MaterialButton btnOrderNow = findViewById(R.id.btnOrderNow);
        if (UserSession.getInstance().isLoggedIn()) {
            btnOrderNow.setText("Order Now");
            btnOrderNow.setOnClickListener(v -> placeOrder());
        } else {
            btnOrderNow.setText(R.string.checkout_login_label);
            btnOrderNow.setOnClickListener(v ->
                    startActivity(new Intent(this, LoginActivity.class)));
        }
    }

    private void placeOrder() {
        User user = UserSession.getInstance().getCurrentUser();
        Cart cart = user.getCart();
        if (cart.getItems().isEmpty()) {
            android.widget.Toast.makeText(this, "Your cart is empty", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        double total = cart.getTotal();
        if (user.getBalance() < total) {
            double shortBy = total - user.getBalance();
            android.widget.Toast.makeText(this,
                    String.format(Locale.US, "Insufficient balance. You need $%.2f more — top up to continue.", shortBy),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        Order order = new Order(
                null,
                user.getId(),
                new ArrayList<>(cart.getItems()),
                Order.Status.CONFIRMED,
                total,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()),
                System.currentTimeMillis()
        );

        UserRepository.getInstance().saveOrder(order, new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order placed) {
                user.getOrders().add(0, placed);
                user.setBalance(user.getBalance() - total);
                UserRepository.getInstance().saveBalance(user);
                cart.clear();
                UserRepository.getInstance().saveCartForCurrentUser();
                android.widget.Toast.makeText(CheckoutActivity.this, "Order placed", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailure(@androidx.annotation.NonNull Exception e) {
                android.widget.Toast.makeText(CheckoutActivity.this,
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Order failed",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
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
        cartItems = UserSession.getInstance().getCurrentUser().getCart().getItems();

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