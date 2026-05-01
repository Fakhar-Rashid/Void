package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.example.avoid.model.CartItem;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Product;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";

    private RecyclerView cartItemsRecyclerView;
    private TextView cartTotalPrice;
    private List<CartItem> cartItems = new ArrayList<>();
    private final Map<String, Product> productsById = new HashMap<>();

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

        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setNestedScrollingEnabled(false);

        loadCart();
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

    private void loadCart() {
        cartItems = UserSession.getInstance().getCurrentUser().getCart().getItems();
        cartItemsRecyclerView.setAdapter(new CartAdapter(cartItems, productsById, this::updateTotal));

        Set<String> ids = new LinkedHashSet<>();
        for (CartItem item : cartItems) {
            if (item.getProductId() != null) ids.add(item.getProductId());
        }
        if (ids.isEmpty()) {
            updateTotal();
            return;
        }

        ProductRepository.getInstance().loadProductsByIds(new ArrayList<>(ids),
                new ProductRepository.Callback<List<Product>>() {
                    @Override public void onSuccess(List<Product> products) {
                        productsById.clear();
                        for (Product p : products) productsById.put(p.getId(), p);
                        cartItemsRecyclerView.setAdapter(new CartAdapter(cartItems, productsById,
                                CheckoutActivity.this::updateTotal));
                        updateTotal();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load products for checkout", e);
                        updateTotal();
                    }
                });
    }

    private void placeOrder() {
        User user = UserSession.getInstance().getCurrentUser();
        Cart cart = user.getCart();
        if (cart.getItems().isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        com.google.android.material.textfield.TextInputEditText addressInput = findViewById(R.id.checkoutAddressInput);
        String address = addressInput.getText() != null ? addressInput.getText().toString().trim() : "";
        if (address.isEmpty()) {
            Toast.makeText(this, "Please provide a shipping address", Toast.LENGTH_LONG).show();
            return;
        }

        if (productsById.isEmpty()) {
            Toast.makeText(this, "Loading products… try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build line items + total from current product snapshots.
        List<OrderLineItem> lineItems = new ArrayList<>();
        List<String> storeIds = new ArrayList<>();
        double total = 0;
        for (CartItem item : cart.getItems()) {
            Product p = productsById.get(item.getProductId());
            if (p == null) continue; // skip missing
            if (p.getStock() < item.getQuantity()) {
                Toast.makeText(this,
                        "Not enough stock for " + p.getName() + " (only " + p.getStock() + " left)",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String img = p.getMainImageUrl();
            lineItems.add(new OrderLineItem(p.getId(), p.getName(), p.getPrice(), img,
                    item.getColor(), item.getQuantity(), p.getStoreId()));
            if (p.getStoreId() != null && !storeIds.contains(p.getStoreId())) {
                storeIds.add(p.getStoreId());
            }
            total += p.getPrice() * item.getQuantity();
        }
        if (lineItems.isEmpty()) {
            Toast.makeText(this, "All cart items are unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user.getBalance() < total) {
            double shortBy = total - user.getBalance();
            Toast.makeText(this,
                    String.format(Locale.US, "Insufficient balance. You need $%.2f more — top up to continue.", shortBy),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final double finalTotal = total;
        final List<OrderLineItem> finalLineItems = lineItems;

        Order order = new Order(
                null,
                user.getId(),
                finalLineItems,
                Order.Status.CONFIRMED,
                finalTotal,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()),
                System.currentTimeMillis(),
                storeIds
        );

        UserRepository.getInstance().saveOrder(order, new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order placed) {
                // Decrement stock per line item (atomic, server-side).
                for (OrderLineItem li : finalLineItems) {
                    ProductRepository.getInstance().decrementStock(li.getProductId(), li.getQuantity());
                }
                user.getOrders().add(0, placed);
                user.setBalance(user.getBalance() - finalTotal);
                UserRepository.getInstance().saveBalance(user);
                cart.clear();
                UserRepository.getInstance().saveCartForCurrentUser();
                Toast.makeText(CheckoutActivity.this, "Order placed", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailure(@NonNull Exception e) {
                Toast.makeText(CheckoutActivity.this,
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Order failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        cartItemsRecyclerView = findViewById(R.id.checkoutItemsRecyclerView);
        cartTotalPrice = findViewById(R.id.checkoutTotalPrice);

        com.google.android.material.textfield.TextInputEditText addressInput = findViewById(R.id.checkoutAddressInput);
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null && user.getAddress() != null) {
            addressInput.setText(user.getAddress());
        }
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

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            Product p = productsById.get(item.getProductId());
            if (p != null) total += p.getPrice() * item.getQuantity();
        }
        cartTotalPrice.setText(String.format(Locale.US, "$ %,.2f", total));
    }
}
