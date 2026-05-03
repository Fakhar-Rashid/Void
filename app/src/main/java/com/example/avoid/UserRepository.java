package com.example.avoid;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.avoid.model.Cart;
import com.example.avoid.model.CartItem;
import com.example.avoid.model.Order;
import com.example.avoid.model.Settings;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore IO for user-scoped data.
 *
 *  users/{uid}     — profile + balance + settings (cart and orders excluded; stored separately)
 *  carts/{uid}     — Cart document
 *  orders          — top-level collection, queried by userId
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final String USERS  = "users";
    private static final String CARTS  = "carts";
    private static final String ORDERS = "orders";
    private static final String STORES = "stores";

    private static UserRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(@NonNull Exception e);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    /**
     * Load (or create on first sign-in) the User document, then attach cart and orders.
     * If a non-empty {@code guestCart} is provided, its items are merged into the user's cart
     * and the cart is persisted before the callback fires.
     */
    public void loadOrCreate(@NonNull FirebaseUser fbUser,
                             @Nullable Cart guestCart,
                             @NonNull Callback<User> callback) {
        String uid = fbUser.getUid();

        db.collection(USERS).document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    User user;
                    if (userDoc.exists()) {
                        user = userDoc.toObject(User.class);
                        if (user == null) user = newUserFromAuth(fbUser);
                    } else {
                        user = newUserFromAuth(fbUser);
                        db.collection(USERS).document(uid).set(user)
                                .addOnFailureListener(e -> Log.e(TAG, "create user failed", e));
                    }
                    user.setId(uid);
                    final User finalUser = user;

                    db.collection(CARTS).document(uid).get()
                            .addOnSuccessListener(cartDoc -> {
                                Cart cart = cartDoc.exists() ? cartDoc.toObject(Cart.class) : new Cart(uid);
                                if (cart == null) cart = new Cart(uid);
                                if (cart.getUserId() == null) cart.setUserId(uid);
                                finalUser.setCart(cart);

                                boolean cartChanged = false;
                                if (guestCart != null && !guestCart.getItems().isEmpty()) {
                                    for (CartItem item : guestCart.getItems()) {
                                        finalUser.getCart().addItem(item.getProductId(), item.getColor(), item.getQuantity());
                                    }
                                    cartChanged = true;
                                }
                                if (cartChanged) saveCart(finalUser);

                                loadOrders(uid, ordersResult -> {
                                    finalUser.setOrders(ordersResult);
                                    loadStoreIfSeller(finalUser, () -> callback.onSuccess(finalUser));
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "load cart failed", e);
                                finalUser.setCart(new Cart(uid));
                                loadOrders(uid, ordersResult -> {
                                    finalUser.setOrders(ordersResult);
                                    loadStoreIfSeller(finalUser, () -> callback.onSuccess(finalUser));
                                });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load user doc failed, falling back to auth-only user", e);
                    User user = newUserFromAuth(fbUser);
                    user.setId(uid);
                    user.setCart(new Cart(uid));
                    user.setOrders(new ArrayList<>());
                    callback.onSuccess(user);
                });
    }

    private void loadOrders(String uid, OrdersDoneListener cb) {
        db.collection(ORDERS)
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        orders.add(o);
                    }
                    Collections.sort(orders, new Comparator<Order>() {
                        @Override public int compare(Order a, Order b) {
                            return Long.compare(b.getOrderTimestamp(), a.getOrderTimestamp());
                        }
                    });
                    cb.onDone(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load orders failed", e);
                    cb.onDone(new ArrayList<>());
                });
    }

    public void loadStoreOrders(String storeId, OrdersDoneListener cb) {
        db.collection(ORDERS)
                .whereArrayContains("storeIds", storeId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> orders = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        orders.add(o);
                    }
                    Collections.sort(orders, new Comparator<Order>() {
                        @Override public int compare(Order a, Order b) {
                            return Long.compare(b.getOrderTimestamp(), a.getOrderTimestamp());
                        }
                    });
                    cb.onDone(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load store orders failed", e);
                    cb.onDone(new ArrayList<>());
                });
    }

    public interface OrdersDoneListener { void onDone(List<Order> orders); }

    private void loadStoreIfSeller(User user, Runnable next) {
        if (!user.isSeller()) { next.run(); return; }
        db.collection(STORES).document(user.getId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) user.setStore(doc.toObject(Store.class));
                    next.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load store failed", e);
                    next.run();
                });
    }

    /** Light user lookup that returns just the profile image URL (or null). Used by the chat
     *  list / chat header to render the other party's avatar without pulling the full doc. */
    public void loadUserProfileImage(@NonNull String userId, @NonNull Callback<String> callback) {
        db.collection(USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    String url = doc.exists() ? doc.getString("profileImageUrl") : null;
                    callback.onSuccess(url);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void loadStore(@NonNull String storeId, @NonNull Callback<Store> callback) {
        db.collection(STORES).document(storeId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new IllegalStateException("Store not found"));
                        return;
                    }
                    callback.onSuccess(doc.toObject(Store.class));
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveStore(@NonNull Store store, @Nullable Callback<Store> callback) {
        if (store.getOwnerId() == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("ownerId required"));
            return;
        }
        db.collection(STORES).document(store.getOwnerId()).set(store)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(store); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "save store failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void saveCart(User user) {
        if (user == null || user.getId() == null || user.getCart() == null) return;
        Cart cart = user.getCart();
        if (cart.getUserId() == null) cart.setUserId(user.getId());
        db.collection(CARTS).document(user.getId()).set(cart)
                .addOnFailureListener(e -> Log.e(TAG, "save cart failed", e));
    }

    public void saveCartForCurrentUser() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn()) saveCart(session.getCurrentUser());
    }

    public void saveSettings(User user) {
        if (user == null || user.getId() == null) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("settings", user.getSettings());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnFailureListener(e -> Log.e(TAG, "save settings failed", e));
    }

    public void saveSettingsForCurrentUser() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn()) saveSettings(session.getCurrentUser());
    }

    public void saveBalance(User user) {
        if (user == null || user.getId() == null) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("balance", user.getBalance());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnFailureListener(e -> Log.e(TAG, "save balance failed", e));
    }

    public void saveProfile(User user, @Nullable Callback<User> callback) {
        if (user == null || user.getId() == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("user/id required"));
            return;
        }
        Map<String, Object> patch = new HashMap<>();
        patch.put("name", user.getName());
        patch.put("phone", user.getPhone());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(user); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "save profile failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    /**
     * Toggle a store in the user's followed-stores list and persist the change. The local
     * list is mutated in-place so the next render sees the new state immediately even if the
     * Firestore write is in flight.
     */
    public void setFollowing(User user, @NonNull String storeId, boolean follow,
                             @Nullable Callback<User> callback) {
        if (user == null || user.getId() == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("user/id required"));
            return;
        }
        List<String> followed = user.getFollowedStoreIds();
        if (follow) {
            if (!followed.contains(storeId)) followed.add(storeId);
        } else {
            followed.remove(storeId);
        }
        Map<String, Object> patch = new HashMap<>();
        patch.put("followedStoreIds", followed);
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(user); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "set following failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    /** Load every store in the given id list. Order of the returned list mirrors {@code storeIds}. */
    public void loadStores(@NonNull List<String> storeIds, @NonNull Callback<List<Store>> callback) {
        if (storeIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        // Firestore "in" supports up to 10 ids per query; for typical follow lists this is plenty.
        // For larger lists we'd need to chunk — left as TODO when that becomes a real problem.
        List<String> trimmed = storeIds.size() > 10 ? storeIds.subList(0, 10) : storeIds;
        db.collection(STORES)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), trimmed)
                .get()
                .addOnSuccessListener(snap -> {
                    java.util.Map<String, Store> byId = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Store s = doc.toObject(Store.class);
                        if (s.getId() == null) s.setId(doc.getId());
                        byId.put(doc.getId(), s);
                    }
                    List<Store> ordered = new ArrayList<>();
                    for (String id : trimmed) {
                        Store s = byId.get(id);
                        if (s != null) ordered.add(s);
                    }
                    callback.onSuccess(ordered);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveAddresses(User user, @Nullable Callback<User> callback) {
        if (user == null || user.getId() == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("user/id required"));
            return;
        }
        Map<String, Object> patch = new HashMap<>();
        patch.put("addresses", user.getAddresses());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(user); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "save addresses failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void saveProfileImage(User user) {
        if (user == null || user.getId() == null) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("profileImageUrl", user.getProfileImageUrl());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnFailureListener(e -> Log.e(TAG, "save profile image failed", e));
    }

    public void saveSellerStatus(User user) {
        if (user == null || user.getId() == null) return;
        Map<String, Object> patch = new HashMap<>();
        patch.put("seller", user.isSeller());
        db.collection(USERS).document(user.getId()).update(patch)
                .addOnFailureListener(e -> Log.e(TAG, "save seller status failed", e));
    }

    public void loadOrder(@NonNull String orderId, @NonNull Callback<Order> callback) {
        db.collection(ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new IllegalStateException("Order not found"));
                        return;
                    }
                    Order o = doc.toObject(Order.class);
                    if (o != null && o.getOrderId() == null) o.setOrderId(doc.getId());
                    callback.onSuccess(o);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveOrder(@NonNull Order order, @Nullable Callback<Order> callback) {
        if (order.getOrderId() == null) {
            db.collection(ORDERS).add(order)
                    .addOnSuccessListener(ref -> {
                        order.setOrderId(ref.getId());
                        if (callback != null) callback.onSuccess(order);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "save order failed", e);
                        if (callback != null) callback.onFailure(e);
                    });
        } else {
            db.collection(ORDERS).document(order.getOrderId()).set(order)
                    .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(order); })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "save order failed", e);
                        if (callback != null) callback.onFailure(e);
                    });
        }
    }

    /** Welcome bonus credited the very first time a Firebase user shows up in Firestore. */
    private static final double NEW_USER_STARTING_BALANCE = 5000.0;

    private static User newUserFromAuth(FirebaseUser fbUser) {
        return new User(
                fbUser.getUid(),
                fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty()
                        ? fbUser.getDisplayName() : "Buyer",
                fbUser.getEmail() != null ? fbUser.getEmail() : "",
                null,
                fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : null,
                NEW_USER_STARTING_BALANCE,
                new Settings()
        );
    }
}
