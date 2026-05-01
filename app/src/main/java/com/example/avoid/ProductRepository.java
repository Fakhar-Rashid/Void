package com.example.avoid;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.avoid.model.Product;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {

    private static final String TAG = "ProductRepository";
    private static final String COLLECTION = "products";

    private static ProductRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(@NonNull Exception e);
    }

    public static synchronized ProductRepository getInstance() {
        if (instance == null) instance = new ProductRepository();
        return instance;
    }

    /** Create or update a product. If product.id is null, a new doc is created and the id is set on the object. */
    public void saveProduct(@NonNull Product product, @Nullable Callback<Product> callback) {
        if (product.getId() == null || product.getId().isEmpty()) {
            db.collection(COLLECTION).add(product)
                    .addOnSuccessListener(ref -> {
                        product.setId(ref.getId());
                        if (callback != null) callback.onSuccess(product);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "create product failed", e);
                        if (callback != null) callback.onFailure(e);
                    });
        } else {
            db.collection(COLLECTION).document(product.getId()).set(product)
                    .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(product); })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "update product failed", e);
                        if (callback != null) callback.onFailure(e);
                    });
        }
    }

    public void deleteProduct(@NonNull String productId, @Nullable Callback<Void> callback) {
        db.collection(COLLECTION).document(productId).delete()
                .addOnSuccessListener(v -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "delete product failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void loadProductsForStore(@NonNull String storeId, @NonNull Callback<List<Product>> callback) {
        db.collection(COLLECTION)
                .whereEqualTo("storeId", storeId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        products.add(doc.toObject(Product.class));
                    }
                    callback.onSuccess(products);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load store products failed", e);
                    callback.onFailure(e);
                });
    }

    public void loadProductsByIds(@NonNull List<String> productIds,
                                  @NonNull Callback<List<Product>> callback) {
        if (productIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        // Firestore 'in' supports up to 10 ids per query. For dev-scale carts this is fine.
        // Larger carts would need to chunk into multiple queries.
        List<String> capped = productIds.size() > 10 ? productIds.subList(0, 10) : productIds;
        db.collection(COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), capped)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        products.add(doc.toObject(Product.class));
                    }
                    callback.onSuccess(products);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "batch load products failed", e);
                    callback.onFailure(e);
                });
    }

    public void loadProduct(@NonNull String productId, @NonNull Callback<Product> callback) {
        db.collection(COLLECTION).document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new IllegalStateException("Product not found"));
                        return;
                    }
                    callback.onSuccess(doc.toObject(Product.class));
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void loadTopProducts(int limit, @NonNull Callback<List<Product>> callback) {
        db.collection(COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        products.add(doc.toObject(Product.class));
                    }
                    callback.onSuccess(products);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Atomically decrements stock. Used during order placement. */
    public void decrementStock(@NonNull String productId, int quantity) {
        db.collection(COLLECTION).document(productId)
                .update("stock", FieldValue.increment(-quantity),
                        "itemsSold", FieldValue.increment(quantity))
                .addOnFailureListener(e -> Log.e(TAG, "stock decrement failed for " + productId, e));
    }
}
