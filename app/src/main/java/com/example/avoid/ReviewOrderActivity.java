package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Review;
import com.example.avoid.model.User;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewOrderActivity extends AppCompatActivity {

    private Order order;
    private boolean isReadOnly;
    private RecyclerView reviewRecyclerView;
    private ReviewProductAdapter adapter;
    private View btnSubmitReview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_order);
        setupSystemBars();

        order = (Order) getIntent().getSerializableExtra("order");
        isReadOnly = getIntent().getBooleanExtra("isReadOnly", false);

        if (order == null || order.getItems().isEmpty()) {
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(isReadOnly ? "Read Review" : "Write a Review");

        reviewRecyclerView = findViewById(R.id.reviewRecyclerView);
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        if (isReadOnly) {
            btnSubmitReview.setVisibility(View.GONE);
            loadExistingReviews();
        } else {
            // Setup for writing reviews
            adapter = new ReviewProductAdapter(order.getItems(), isReadOnly, null);
            reviewRecyclerView.setAdapter(adapter);

            btnSubmitReview.setOnClickListener(v -> submitReviews());
        }
    }

    private void loadExistingReviews() {
        ReviewRepository.getInstance().loadReviewsByOrderId(order.getOrderId(), new ReviewRepository.Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> result) {
                // Map product ID to review for easy lookup
                Map<String, Review> reviewMap = new HashMap<>();
                for (Review r : result) {
                    reviewMap.put(r.getProductId(), r);
                }
                adapter = new ReviewProductAdapter(order.getItems(), isReadOnly, reviewMap);
                reviewRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ReviewOrderActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReviews() {
        if (adapter == null) return;
        List<ReviewProductAdapter.ReviewData> dataList = adapter.getReviewData();
        
        List<Review> reviewsToSave = new ArrayList<>();
        User currentUser = UserSession.getInstance().getCurrentUser();
        String reviewerName = currentUser != null ? currentUser.getName() : "Anonymous";
        String dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());

        for (ReviewProductAdapter.ReviewData data : dataList) {
            // Only save if they actually rated it (rating > 0)
            if (data.rating > 0) {
                reviewsToSave.add(new Review(
                        reviewerName,
                        data.rating,
                        dateStr,
                        data.comment,
                        data.productId,
                        order.getOrderId()
                ));
            }
        }

        if (reviewsToSave.isEmpty()) {
            Toast.makeText(this, "Please rate at least one item.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
        ReviewRepository.getInstance().saveReviews(reviewsToSave, order, new ReviewRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ReviewOrderActivity.this, "Reviews submitted!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                btnSubmitReview.setEnabled(true);
                Toast.makeText(ReviewOrderActivity.this, "Failed to submit reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private static class ReviewProductAdapter extends RecyclerView.Adapter<ReviewProductAdapter.ViewHolder> {
        
        static class ReviewData {
            String productId;
            float rating = 0;
            String comment = "";
            ReviewData(String pid) { this.productId = pid; }
        }

        private final List<OrderLineItem> items;
        private final boolean isReadOnly;
        private final Map<String, Review> reviewMap;
        private final List<ReviewData> reviewDataList;

        ReviewProductAdapter(List<OrderLineItem> items, boolean isReadOnly, Map<String, Review> reviewMap) {
            this.items = items;
            this.isReadOnly = isReadOnly;
            this.reviewMap = reviewMap;
            this.reviewDataList = new ArrayList<>();
            for (OrderLineItem item : items) {
                reviewDataList.add(new ReviewData(item.getProductId()));
            }
        }

        List<ReviewData> getReviewData() {
            return reviewDataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrderLineItem item = items.get(position);
            ReviewData data = reviewDataList.get(position);
            
            holder.productName.setText(item.getProductName());
            
            if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getProductImageUrl())
                        .transform(new CenterCrop(), new RoundedCorners(8))
                        .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(0);
            }

            if (isReadOnly) {
                holder.ratingBar.setIsIndicator(true);
                holder.commentInput.setEnabled(false);
                
                Review existingReview = reviewMap != null ? reviewMap.get(item.getProductId()) : null;
                if (existingReview != null) {
                    holder.ratingBar.setRating(existingReview.getRating());
                    holder.commentInput.setText(existingReview.getComment());
                } else {
                    holder.ratingBar.setRating(0);
                    holder.commentInput.setText("No review provided.");
                }
            } else {
                holder.ratingBar.setIsIndicator(false);
                holder.commentInput.setEnabled(true);
                
                holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                    if (fromUser) data.rating = rating;
                });
                
                holder.commentInput.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        data.comment = s.toString();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName;
            RatingBar ratingBar;
            TextInputEditText commentInput;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.reviewProductImage);
                productName = itemView.findViewById(R.id.reviewProductName);
                ratingBar = itemView.findViewById(R.id.reviewRatingBar);
                commentInput = itemView.findViewById(R.id.reviewCommentInput);
            }
        }
    }
}
