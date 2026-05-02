package com.example.avoid;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Product;
import com.example.avoid.model.Review;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {

    private static final String TAG = "ReviewRepository";
    private static final String COLLECTION = "reviews";
    private static final String ORDERS = "orders";
    private static final String PRODUCTS = "products";

    private static ReviewRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(@NonNull Exception e);
    }

    public static synchronized ReviewRepository getInstance() {
        if (instance == null) instance = new ReviewRepository();
        return instance;
    }

    /** Loads reviews by id. Firestore 'in' caps at 10 per query — we chunk transparently. */
    public void loadReviewsByIds(@NonNull List<String> reviewIds, @NonNull Callback<List<Review>> callback) {
        if (reviewIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        List<String> capped = reviewIds.size() > 10 ? reviewIds.subList(0, 10) : reviewIds;
        db.collection(COLLECTION)
                .whereIn(FieldPath.documentId(), capped)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) reviews.add(doc.toObject(Review.class));
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load reviews failed", e);
                    callback.onFailure(e);
                });
    }

    public void loadReviewsByOrderId(@NonNull String orderId, @NonNull Callback<List<Review>> callback) {
        db.collection(COLLECTION)
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) reviews.add(doc.toObject(Review.class));
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load reviews by orderId failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Saves a per-product review and atomically:
     * <ol>
     *   <li>Writes a {@code reviews/{newId}} doc with rating + issues + comment.</li>
     *   <li>Flips {@code reviewed = true} on the matching {@link OrderLineItem}.</li>
     *   <li>Persists the updated order doc.</li>
     *   <li>Appends the review id to the product's {@code reviewIds} array and
     *       updates the product's running average rating.</li>
     * </ol>
     * The supplied {@code order} is mutated in place so the caller can re-render immediately.
     */
    public void saveProductReview(@NonNull Review review,
                                  @NonNull Order order,
                                  @NonNull Callback<Void> callback) {
        if (review.getProductId() == null) {
            callback.onFailure(new IllegalArgumentException("productId is required"));
            return;
        }
        final String productId = review.getProductId();
        final DocumentReference reviewRef = db.collection(COLLECTION).document();
        review.setId(reviewRef.getId());

        // Update the matching line item's reviewed flag in memory so the caller sees it immediately.
        for (OrderLineItem item : order.getItems()) {
            if (productId.equals(item.getProductId())) item.setReviewed(true);
        }

        db.runTransaction(transaction -> {
            DocumentReference productRef = db.collection(PRODUCTS).document(productId);
            DocumentSnapshot productSnap = transaction.get(productRef);

            transaction.set(reviewRef, review);
            transaction.set(db.collection(ORDERS).document(order.getOrderId()), order);

            if (productSnap.exists()) {
                Product p = productSnap.toObject(Product.class);
                if (p != null) {
                    int prior = p.getReviewIds().size();
                    p.getReviewIds().add(review.getId());
                    double newAverage = ((p.getRating() * prior) + review.getRating()) / (prior + 1);
                    p.setRating(newAverage);
                    transaction.set(productRef, p);
                }
            }
            return null;
        }).addOnSuccessListener(v -> callback.onSuccess(null))
          .addOnFailureListener(e -> {
              Log.e(TAG, "save product review failed", e);
              callback.onFailure(e);
          });
    }
}
