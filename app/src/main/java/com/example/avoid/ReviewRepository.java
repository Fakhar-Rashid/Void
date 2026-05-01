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
}
