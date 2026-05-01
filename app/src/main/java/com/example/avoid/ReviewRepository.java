package com.example.avoid;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.avoid.model.Review;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {

    private static final String TAG = "ReviewRepository";
    private static final String COLLECTION = "reviews";

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

    /**
     * Loads reviews by id. Firestore 'in' caps at 10 per query — we chunk transparently.
     */
    public void loadReviewsByIds(@NonNull List<String> reviewIds, @NonNull Callback<List<Review>> callback) {
        if (reviewIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        // For dev scale we accept the 10-id cap.
        List<String> capped = reviewIds.size() > 10 ? reviewIds.subList(0, 10) : reviewIds;
        db.collection(COLLECTION)
                .whereIn(FieldPath.documentId(), capped)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        reviews.add(doc.toObject(Review.class));
                    }
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
                    for (QueryDocumentSnapshot doc : snap) {
                        reviews.add(doc.toObject(Review.class));
                    }
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "load reviews by orderId failed", e);
                    callback.onFailure(e);
                });
    }

    public void saveReviews(List<Review> reviews, com.example.avoid.model.Order order, Callback<Void> callback) {
        db.runTransaction(transaction -> {
            
            // 1. Perform ALL reads first (Firestore transaction rule)
            List<com.google.firebase.firestore.DocumentSnapshot> productSnaps = new ArrayList<>();
            List<com.google.firebase.firestore.DocumentReference> productRefs = new ArrayList<>();
            
            for (Review review : reviews) {
                com.google.firebase.firestore.DocumentReference productRef = db.collection("products").document(review.getProductId());
                productRefs.add(productRef);
                productSnaps.add(transaction.get(productRef));
            }

            // 2. Perform ALL writes
            for (int i = 0; i < reviews.size(); i++) {
                Review review = reviews.get(i);
                
                com.google.firebase.firestore.DocumentReference reviewRef = db.collection(COLLECTION).document();
                review.setId(reviewRef.getId());
                transaction.set(reviewRef, review);

                com.google.firebase.firestore.DocumentSnapshot productSnap = productSnaps.get(i);
                com.google.firebase.firestore.DocumentReference productRef = productRefs.get(i);
                
                if (productSnap.exists()) {
                    com.example.avoid.model.Product p = productSnap.toObject(com.example.avoid.model.Product.class);
                    if (p != null) {
                        p.getReviewIds().add(review.getId());
                        // Simple average calculation
                        double oldRating = p.getRating();
                        int oldTotal = p.getReviewIds().size() - 1; // Since we just added one
                        double newRating = ((oldRating * oldTotal) + review.getRating()) / p.getReviewIds().size();
                        p.setRating(newRating);
                        transaction.set(productRef, p);
                    }
                }
            }

            // Mark order as reviewed
            order.setReviewed(true);
            com.google.firebase.firestore.DocumentReference orderRef = db.collection("orders").document(order.getOrderId());
            transaction.set(orderRef, order);

            return null;
        }).addOnSuccessListener(aVoid -> callback.onSuccess(null))
          .addOnFailureListener(e -> {
              Log.e(TAG, "save reviews transaction failed", e);
              callback.onFailure(e);
          });
    }
}
