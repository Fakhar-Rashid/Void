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
import com.example.avoid.model.OrderLineItem;
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
 * Per-product review bottom sheet. Opened from {@link OrderDetailsFragment} for a specific
 * {@link OrderLineItem}; saves a {@link Review} pinned to that {@code productId} and flips
 * the matching line item's {@code reviewed} flag via {@link ReviewRepository#saveProductReview}.
 */
public class ReviewOrderBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ReviewOrderBottomSheet";
    private static final String ARG_ORDER         = "order";
    private static final String ARG_PRODUCT_ID    = "productId";
    private static final String ARG_PRODUCT_NAME  = "productName";
    private static final String ARG_STORE_ID      = "storeId";
    private static final String ARG_READ_ONLY     = "readOnly";

    private static final int LOW_RATING_THRESHOLD = 3;

    private Order order;
    private String productId;
    private String productName;
    private String storeId;
    private boolean readOnly;

    private TextView title, subtitle, rateLabel, commentReadOnly;
    private ImageView[] stars;
    private int currentRating = 0;
    private boolean ratingEnabled = true;
    private View issuesContainer;
    private MaterialCheckBox issue1, issue2, issue3, issue4;
    private TextInputLayout commentLayout;
    private TextInputEditText commentInput;
    private MaterialButton submitButton;

    @Nullable private Runnable onSubmitted;

    public static ReviewOrderBottomSheet newInstanceForItem(@NonNull Order order,
                                                            @NonNull OrderLineItem item,
                                                            boolean readOnly) {
        ReviewOrderBottomSheet sheet = new ReviewOrderBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        args.putString(ARG_PRODUCT_ID, item.getProductId());
        args.putString(ARG_PRODUCT_NAME, item.getProductName());
        args.putString(ARG_STORE_ID, item.getStoreId());
        args.putBoolean(ARG_READ_ONLY, readOnly);
        sheet.setArguments(args);
        return sheet;
    }

    public static void showForItem(@NonNull FragmentManager fm,
                                   @NonNull Order order,
                                   @NonNull OrderLineItem item,
                                   boolean readOnly,
                                   @Nullable Runnable onSubmitted) {
        ReviewOrderBottomSheet sheet = newInstanceForItem(order, item, readOnly);
        sheet.onSubmitted = onSubmitted;
        sheet.show(fm, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_order_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) { dismiss(); return; }
        order = (Order) args.getSerializable(ARG_ORDER);
        productId   = args.getString(ARG_PRODUCT_ID);
        productName = args.getString(ARG_PRODUCT_NAME);
        storeId     = args.getString(ARG_STORE_ID);
        readOnly    = args.getBoolean(ARG_READ_ONLY, false);
        if (order == null || productId == null) { dismiss(); return; }

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

        title.setText(readOnly ? "Your review" : "How was this product?");
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
        // Always start with a usable fallback so the sheet has something while the store name loads.
        String fallback = productName != null ? productName : "this product";
        subtitle.setText(fallback);

        if (storeId == null) return;
        UserRepository.getInstance().loadStore(storeId,
                new UserRepository.Callback<com.example.avoid.model.Store>() {
                    @Override
                    public void onSuccess(com.example.avoid.model.Store result) {
                        if (!isAdded() || result == null || result.getName() == null) return;
                        subtitle.setText(fallback + " · From " + result.getName());
                    }
                    @Override public void onFailure(@NonNull Exception e) {}
                });
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
        rateLabel.setText("Rate this product");
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
                        if (!isAdded()) return;
                        Review match = null;
                        for (Review r : result) {
                            if (productId.equals(r.getProductId())) { match = r; break; }
                        }
                        if (match == null) return;
                        setRating(Math.round(match.getRating()));

                        List<String> issues = match.getIssues();
                        if (issues != null && !issues.isEmpty()) {
                            issuesContainer.setVisibility(View.VISIBLE);
                            for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
                                cb.setChecked(issues.contains(cb.getText().toString()));
                            }
                        } else {
                            issuesContainer.setVisibility(View.GONE);
                        }

                        if (match.getComment() != null && !match.getComment().isEmpty()) {
                            commentReadOnly.setVisibility(View.VISIBLE);
                            commentReadOnly.setText("“" + match.getComment() + "”");
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

        Review review = new Review(reviewerName, currentRating, dateStr, comment, productId, order.getOrderId());
        if (currentRating < LOW_RATING_THRESHOLD) {
            java.util.List<String> issues = new java.util.ArrayList<>();
            for (MaterialCheckBox cb : new MaterialCheckBox[]{issue1, issue2, issue3, issue4}) {
                if (cb.isChecked()) issues.add(cb.getText().toString());
            }
            review.setIssues(issues);
        }

        submitButton.setEnabled(false);
        submitButton.setText("Submitting…");

        ReviewRepository.getInstance().saveProductReview(review, order, new ReviewRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Thanks for your review!", Toast.LENGTH_SHORT).show();
                if (onSubmitted != null) onSubmitted.run();
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
