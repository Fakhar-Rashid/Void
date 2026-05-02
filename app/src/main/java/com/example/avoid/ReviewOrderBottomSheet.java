package com.example.avoid;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.example.avoid.model.Order;
import com.example.avoid.model.Review;
import com.example.avoid.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Bottom-sheet review experience that opens from the orders list when an order is delivered.
 * <p>
 * Saves a single {@link Review} per order — rating + delivery issues + optional comment —
 * and marks the order as reviewed atomically via {@link ReviewRepository#saveOrderReview}.
 */
public class ReviewOrderBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ReviewOrderBottomSheet";
    private static final String ARG_ORDER     = "order";
    private static final String ARG_READ_ONLY = "readOnly";

    private Order order;
    private boolean readOnly;

    private static final int LOW_RATING_THRESHOLD = 3;

    private TextView title, subtitle, rateLabel, commentReadOnly;
    private ImageView[] stars;
    private int currentRating = 0;
    private boolean ratingEnabled = true;
    private View issuesContainer;
    private MaterialCheckBox issue1, issue2, issue3, issue4;
    private TextInputLayout commentLayout;
    private TextInputEditText commentInput;
    private MaterialButton submitButton;

    public static ReviewOrderBottomSheet newInstance(@NonNull Order order, boolean readOnly) {
        ReviewOrderBottomSheet sheet = new ReviewOrderBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        args.putBoolean(ARG_READ_ONLY, readOnly);
        sheet.setArguments(args);
        return sheet;
    }

    public static void show(@NonNull FragmentManager fm, @NonNull Order order, boolean readOnly) {
        newInstance(order, readOnly).show(fm, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_order_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }
        order = (Order) args.getSerializable(ARG_ORDER);
        readOnly = args.getBoolean(ARG_READ_ONLY, false);
        if (order == null) { dismiss(); return; }

        title           = view.findViewById(R.id.reviewSheetTitle);
        subtitle        = view.findViewById(R.id.reviewSheetSubtitle);
        rateLabel       = view.findViewById(R.id.reviewSheetRateLabel);
        stars = new ImageView[]{
                view.findViewById(R.id.reviewStar1),
                view.findViewById(R.id.reviewStar2),
                view.findViewById(R.id.reviewStar3),
                view.findViewById(R.id.reviewStar4),
                view.findViewById(R.id.reviewStar5)
        };
        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> {
                if (ratingEnabled) setRating(rating);
            });
        }
        issuesContainer = view.findViewById(R.id.reviewSheetIssuesContainer);
        issue1          = view.findViewById(R.id.reviewSheetIssue1);
        issue2          = view.findViewById(R.id.reviewSheetIssue2);
        issue3          = view.findViewById(R.id.reviewSheetIssue3);
        issue4          = view.findViewById(R.id.reviewSheetIssue4);
        commentLayout   = view.findViewById(R.id.reviewSheetCommentLayout);
        commentInput    = view.findViewById(R.id.reviewSheetCommentInput);
        commentReadOnly = view.findViewById(R.id.reviewSheetCommentReadOnly);
        submitButton    = view.findViewById(R.id.reviewSheetSubmit);

        title.setText(readOnly ? "Your review" : "Order Delivered!");
        bindSubtitle();

        if (readOnly) {
            applyReadOnlyMode();
            loadExistingReview();
        } else {
            applyEditableMode();
            submitButton.setOnClickListener(v -> submit());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the dialog's frame transparent so the sheet's rounded corners + the
        // floating top-icon overlap render correctly against the dimmed scrim.
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            View bottomSheetFrame = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetFrame != null) {
                bottomSheetFrame.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                if (bottomSheetFrame.getParent() instanceof ViewGroup) {
                    ((ViewGroup) bottomSheetFrame).setClipChildren(false);
                    ((ViewGroup) bottomSheetFrame).setClipToPadding(false);
                }
            }
        }
    }

    private void bindSubtitle() {
        String store = order.getStoreIds() != null && !order.getStoreIds().isEmpty()
                ? "your store" : "your store";
        String date = order.getOrderDate() != null ? order.getOrderDate()
                : DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
        String fallback = "Your order from " + store + " arrived on " + date + ".";
        subtitle.setText(fallback);

        // Resolve actual store name asynchronously and patch the subtitle in.
        if (order.getStoreIds() != null && !order.getStoreIds().isEmpty()) {
            UserRepository.getInstance().loadStore(order.getStoreIds().get(0),
                    new UserRepository.Callback<com.example.avoid.model.Store>() {
                        @Override
                        public void onSuccess(com.example.avoid.model.Store result) {
                            if (!isAdded() || result == null || result.getName() == null) return;
                            subtitle.setText("Your order from " + result.getName() + " arrived on " + date + ".");
                        }
                        @Override public void onFailure(@NonNull Exception e) { /* keep fallback */ }
                    });
        }
    }

    private void applyReadOnlyMode() {
        rateLabel.setText("Your rating");
        ratingEnabled = false;
        commentLayout.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
            cb.setEnabled(false);
        }
    }

    private void applyEditableMode() {
        rateLabel.setText("Rate your order");
        ratingEnabled = true;
        setRating(0);
        commentLayout.setVisibility(View.VISIBLE);
        commentReadOnly.setVisibility(View.GONE);
        submitButton.setVisibility(View.VISIBLE);
    }

    private void setRating(int rating) {
        currentRating = Math.max(0, Math.min(5, rating));
        int filled = ContextCompat.getColor(requireContext(), R.color.review_star_filled);
        int empty  = ContextCompat.getColor(requireContext(), R.color.review_star_empty);
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < currentRating ? filled : empty);
        }

        // In editable mode, the issues block only matters for low ratings.
        if (ratingEnabled && issuesContainer != null) {
            boolean show = currentRating > 0 && currentRating < LOW_RATING_THRESHOLD;
            issuesContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) clearIssueChecks();
        }
    }

    private void clearIssueChecks() {
        for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
            cb.setChecked(false);
        }
    }

    private void loadExistingReview() {
        ReviewRepository.getInstance().loadReviewsByOrderId(order.getOrderId(),
                new ReviewRepository.Callback<List<Review>>() {
                    @Override
                    public void onSuccess(List<Review> result) {
                        if (!isAdded() || result.isEmpty()) return;
                        Review r = result.get(0);
                        setRating(Math.round(r.getRating()));

                        List<String> issues = r.getIssues();
                        if (issues != null && !issues.isEmpty()) {
                            issuesContainer.setVisibility(View.VISIBLE);
                            for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
                                cb.setChecked(issues.contains(cb.getText().toString()));
                            }
                        } else {
                            issuesContainer.setVisibility(View.GONE);
                        }

                        if (r.getComment() != null && !r.getComment().isEmpty()) {
                            commentReadOnly.setVisibility(View.VISIBLE);
                            commentReadOnly.setText("“" + r.getComment() + "”");
                        }
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
    }

    private void submit() {
        if (currentRating <= 0) {
            Toast.makeText(requireContext(), "Please tap a star to rate", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = commentInput.getText() != null ? commentInput.getText().toString().trim() : "";

        User currentUser = UserSession.getInstance().getCurrentUser();
        String reviewerName = currentUser != null && currentUser.getName() != null
                ? currentUser.getName() : "Anonymous";
        String dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());

        Review review = new Review(reviewerName, currentRating, dateStr, comment, null, order.getOrderId());

        // Only collect issue tags for low ratings — matches how the UI exposes them.
        if (currentRating < LOW_RATING_THRESHOLD) {
            java.util.List<String> issues = new java.util.ArrayList<>();
            for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
                if (cb.isChecked()) issues.add(cb.getText().toString());
            }
            review.setIssues(issues);
        }

        submitButton.setEnabled(false);
        submitButton.setText("Submitting…");

        ReviewRepository.getInstance().saveOrderReview(review, order, new ReviewRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Thanks for your review!", Toast.LENGTH_SHORT).show();
                dismiss();
            }
            @Override
            public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                submitButton.setEnabled(true);
                submitButton.setText("Submit");
                Toast.makeText(requireContext(),
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Failed to submit review",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
