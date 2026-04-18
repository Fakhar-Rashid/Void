package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.model.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final RatingBar ratingBar;
        private final TextView dateTextView;
        private final TextView commentTextView;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.reviewName);
            ratingBar = itemView.findViewById(R.id.reviewRatingBar);
            dateTextView = itemView.findViewById(R.id.reviewDate);
            commentTextView = itemView.findViewById(R.id.reviewComment);
        }

        void bind(Review review) {
            nameTextView.setText(review.getReviewerName());
            ratingBar.setRating(review.getRating());
            dateTextView.setText(review.getDate());
            commentTextView.setText(review.getComment());
        }
    }
}
