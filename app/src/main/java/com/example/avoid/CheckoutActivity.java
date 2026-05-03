package com.example.avoid;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.CartAdapter;
import com.example.avoid.model.Address;
import com.example.avoid.model.Cart;
import com.example.avoid.model.CartItem;
import com.example.avoid.model.NotificationItem;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Product;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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

    private LinearLayout addressList;
    private View addressForm;
    private TextView addressManageHint;
    private TextInputEditText inputHouseNumber, inputStreetNumber, inputArea, inputCountry;
    private AutoCompleteTextView inputProvince;

    private RadioButton radioEzpay;
    private RadioButton radioCOD;

    private static final ColorStateList BLACK_TINT = ColorStateList.valueOf(Color.BLACK);

    /** Index into the user's saved address list — only meaningful when the list view is shown. */
    private int selectedAddressIndex = 0;

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
        renderShipping();
        renderPaymentOptions();
        configureCheckoutButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderShipping();
        renderPaymentOptions();
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

    private void renderShipping() {
        User user = UserSession.getInstance().getCurrentUser();
        boolean loggedIn = UserSession.getInstance().isLoggedIn();
        boolean hasSaved = loggedIn && user.hasSavedAddresses();

        addressList.setVisibility(hasSaved ? View.VISIBLE : View.GONE);
        addressManageHint.setVisibility(hasSaved ? View.VISIBLE : View.GONE);
        addressForm.setVisibility(hasSaved ? View.GONE : View.VISIBLE);

        if (hasSaved) bindSavedAddresses(user.getAddresses());
    }

    private void bindSavedAddresses(List<Address> addresses) {
        addressList.removeAllViews();
        if (selectedAddressIndex >= addresses.size()) selectedAddressIndex = 0;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < addresses.size(); i++) {
            Address addr = addresses.get(i);
            View row = inflater.inflate(R.layout.item_checkout_address, addressList, false);
            RadioButton radio = row.findViewById(R.id.checkoutAddressRadio);
            TextView label = row.findViewById(R.id.checkoutAddressLabel);
            TextView body  = row.findViewById(R.id.checkoutAddressBody);

            CompoundButtonCompat.setButtonTintList(radio, BLACK_TINT);
            radio.setChecked(i == selectedAddressIndex);
            // The radio is purely a visual indicator — taps bubble up to the row,
            // which is the single source of truth for which address is selected.
            radio.setClickable(false);
            radio.setFocusable(false);
            label.setText("Address " + (i + 1));
            body.setText(addr.getMultiLine());

            final int index = i;
            row.setOnClickListener(v -> {
                selectedAddressIndex = index;
                for (int j = 0; j < addressList.getChildCount(); j++) {
                    RadioButton rb = addressList.getChildAt(j).findViewById(R.id.checkoutAddressRadio);
                    if (rb != null) rb.setChecked(j == index);
                }
            });

            addressList.addView(row);
        }
    }

    private void renderPaymentOptions() {
        User user = UserSession.getInstance().getCurrentUser();
        TextView ezpayBalance = findViewById(R.id.checkoutEzpayBalance);
        ezpayBalance.setText(String.format(Locale.US, "Balance $%,.2f", user.getBalance()));
    }

    private Address readAddressForm() {
        return new Address(
                text(inputHouseNumber),
                text(inputStreetNumber),
                text(inputArea),
                text(inputProvince),
                Address.DEFAULT_COUNTRY
        );
    }

    private static String text(EditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
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

        boolean usingSaved = user.hasSavedAddresses();
        Address shipping;
        Address newAddressToPersist = null;
        if (usingSaved) {
            int idx = Math.max(0, Math.min(selectedAddressIndex, user.getAddresses().size() - 1));
            shipping = user.getAddresses().get(idx);
        } else {
            shipping = readAddressForm();
            if (!shipping.isComplete()) {
                Toast.makeText(this, "Please fill in all address fields", Toast.LENGTH_LONG).show();
                return;
            }
            newAddressToPersist = shipping;
        }

        if (productsById.isEmpty()) {
            Toast.makeText(this, "Loading products… try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        String paymentMethod = radioCOD.isChecked() ? Order.PAYMENT_COD : Order.PAYMENT_EZPAY;

        long now = System.currentTimeMillis();
        List<OrderLineItem> lineItems = new ArrayList<>();
        List<String> storeIds = new ArrayList<>();
        double total = 0;
        for (CartItem item : cart.getItems()) {
            Product p = productsById.get(item.getProductId());
            if (p == null) continue;
            if (p.getStock() < item.getQuantity()) {
                Toast.makeText(this,
                        "Not enough stock for " + p.getName() + " (only " + p.getStock() + " left)",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String img = p.getMainImageUrl();
            OrderLineItem line = new OrderLineItem(p.getId(), p.getName(), p.getPrice(), img,
                    item.getColor(), item.getQuantity(), p.getStoreId());
            line.setStatus(Order.Status.CONFIRMED);
            line.setConfirmedAt(now);
            lineItems.add(line);
            if (p.getStoreId() != null && !storeIds.contains(p.getStoreId())) {
                storeIds.add(p.getStoreId());
            }
            total += p.getPrice() * item.getQuantity();
        }
        if (lineItems.isEmpty()) {
            Toast.makeText(this, "All cart items are unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ezpay requires sufficient balance; COD does not.
        if (Order.PAYMENT_EZPAY.equals(paymentMethod) && user.getBalance() < total) {
            double shortBy = total - user.getBalance();
            Toast.makeText(this,
                    String.format(Locale.US, "Insufficient balance. You need $%.2f more — top up or pick Cash on Delivery.", shortBy),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final double finalTotal = total;
        final List<OrderLineItem> finalLineItems = lineItems;
        final String finalPayment = paymentMethod;
        final Address finalAddressToPersist = newAddressToPersist;

        Order order = new Order(
                null,
                user.getId(),
                finalLineItems,
                finalTotal,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()),
                now,
                storeIds,
                shipping,
                finalPayment
        );
        order.setBuyerName(user.getName());
        order.setBuyerEmail(user.getEmail());
        order.setBuyerPhone(user.getPhone());

        UserRepository.getInstance().saveOrder(order, new UserRepository.Callback<Order>() {
            @Override public void onSuccess(Order placed) {
                for (OrderLineItem li : finalLineItems) {
                    ProductRepository.getInstance().decrementStock(li.getProductId(), li.getQuantity());
                }
                user.getOrders().add(0, placed);
                if (Order.PAYMENT_EZPAY.equals(finalPayment)) {
                    user.setBalance(user.getBalance() - finalTotal);
                    UserRepository.getInstance().saveBalance(user);
                }
                if (finalAddressToPersist != null && user.canAddAddress()) {
                    user.getAddresses().add(finalAddressToPersist);
                    UserRepository.getInstance().saveAddresses(user, null);
                }

                // Notify every store that contributed to this order.
                for (String storeId : placed.getStoreIds()) {
                    if (storeId == null) continue;
                    NotificationItem n = new NotificationItem();
                    n.setType(NotificationItem.TYPE_ORDER_NEW);
                    n.setTitle("New order");
                    n.setBody((user.getName() != null ? user.getName() : "A buyer")
                            + " placed an order with your store.");
                    n.setOrderId(placed.getOrderId());
                    NotificationRepository.getInstance().send(storeId, n);
                }

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

        addressList         = findViewById(R.id.checkoutAddressList);
        addressForm         = findViewById(R.id.checkoutAddressForm);
        addressManageHint   = findViewById(R.id.checkoutAddressManageHint);
        inputHouseNumber    = findViewById(R.id.inputHouseNumber);
        inputStreetNumber   = findViewById(R.id.inputStreetNumber);
        inputArea           = findViewById(R.id.inputArea);
        inputProvince       = findViewById(R.id.inputProvince);
        inputCountry        = findViewById(R.id.inputCountry);

        inputProvince.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Address.PAKISTAN_PROVINCES));

        radioEzpay          = findViewById(R.id.radioEzpay);
        radioCOD            = findViewById(R.id.radioCOD);
        CompoundButtonCompat.setButtonTintList(radioEzpay, BLACK_TINT);
        CompoundButtonCompat.setButtonTintList(radioCOD,   BLACK_TINT);
        radioEzpay.setOnClickListener(v -> { radioEzpay.setChecked(true); radioCOD.setChecked(false); });
        radioCOD.setOnClickListener(v ->   { radioCOD.setChecked(true);   radioEzpay.setChecked(false); });
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
