package com.example.avoid.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.avoid.R;
import com.example.avoid.model.Order;
import com.example.avoid.model.OrderLineItem;
import com.example.avoid.model.Review;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders {@link OrderLineItem}s with their own progress strip + per-item action button.
 * Used in both:
 * <ul>
 *   <li>Buyer's Order Details — action button is "Leave a review"; once reviewed the
 *       button is replaced by an inline preview of the review (rating + comment).</li>
 *   <li>Seller's Orders — action button advances the line item's status.</li>
 * </ul>
 */
public class OrderLineAdapter extends RecyclerView.Adapter<OrderLineAdapter.LineViewHolder> {

    public enum Mode { BUYER, SELLER }

    public interface ActionListener {
        void onAction(OrderLineItem item, int position);
    }

    public interface ItemClickListener {
        void onItemClick(OrderLineItem item, int position);
    }

    private final List<OrderLineItem> items;
    private final Mode mode;
    private final ActionListener listener;
    @Nullable private ItemClickListener itemClickListener;
    private Map<String, Review> reviewsByProductId = new HashMap<>();

    public OrderLineAdapter(List<OrderLineItem> items, Mode mode, ActionListener listener) {
        this.items = items;
        this.mode = mode;
        this.listener = listener;
    }

    public void setItemClickListener(@Nullable ItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setReviews(@NonNull Map<String, Review> reviews) {
        this.reviewsByProductId = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_line, parent, false);
        return new LineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
        OrderLineItem item = items.get(position);
        Review review = reviewsByProductId.get(item.getProductId());
        holder.bind(item, mode, listener, itemClickListener, review, position);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class LineViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView image;
        private final TextView name, meta, store, price, statusText;
        private final View seg1, seg2, seg3, seg4;
        private final MaterialButton actionButton;
        private final View reviewDisplay;
        private final ImageView star1, star2, star3, star4, star5;
        private final TextView reviewComment;

        LineViewHolder(@NonNull View itemView) {
            super(itemView);
            image          = itemView.findViewById(R.id.orderLineImage);
            name           = itemView.findViewById(R.id.orderLineName);
            meta           = itemView.findViewById(R.id.orderLineMeta);
            store          = itemView.findViewById(R.id.orderLineStore);
            price          = itemView.findViewById(R.id.orderLinePrice);
            seg1           = itemView.findViewById(R.id.orderLineSeg1);
            seg2           = itemView.findViewById(R.id.orderLineSeg2);
            seg3           = itemView.findViewById(R.id.orderLineSeg3);
            seg4           = itemView.findViewById(R.id.orderLineSeg4);
            statusText     = itemView.findViewById(R.id.orderLineStatusText);
            actionButton   = itemView.findViewById(R.id.orderLineActionButton);
            reviewDisplay  = itemView.findViewById(R.id.orderLineReviewDisplay);
            star1          = itemView.findViewById(R.id.orderLineStar1);
            star2          = itemView.findViewById(R.id.orderLineStar2);
            star3          = itemView.findViewById(R.id.orderLineStar3);
            star4          = itemView.findViewById(R.id.orderLineStar4);
            star5          = itemView.findViewById(R.id.orderLineStar5);
            reviewComment  = itemView.findViewById(R.id.orderLineReviewComment);
        }

        void bind(OrderLineItem item, Mode mode, ActionListener actionListener,
                  @Nullable ItemClickListener itemClickListener,
                  @Nullable Review existingReview,
                  int position) {
            Context ctx = itemView.getContext();

            name.setText(item.getProductName());
            meta.setText((item.getColor() != null ? item.getColor() : "") + " · Qty " + item.getQuantity());
            price.setText(item.getDisplayPrice());

            if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
                Glide.with(ctx).load(item.getProductImageUrl())
                        .placeholder(R.drawable.bg_product_placeholder)
                        .error(R.drawable.bg_product_placeholder)
                        .into(image);
            } else {
                image.setImageDrawable(null);
            }

            store.setVisibility(View.GONE); // populated async by caller via setStoreName.

            bindProgress(ctx, item.getStatus());
            statusText.setText(buildStatusText(item));

            if (mode == Mode.BUYER) {
                bindBuyerView(item, actionListener, existingReview, position);
            } else {
                bindSellerAction(item, actionListener, position);
            }

            // Forward taps anywhere on the card to the item-click listener (skip when over
            // the action button, which has its own click handler).
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onItemClick(item, position);
            });
        }

        private void bindBuyerView(OrderLineItem item, ActionListener actionListener,
                                   @Nullable Review existingReview, int position) {
            // Not yet delivered → no review/action UI at all.
            if (item.getStatus() != Order.Status.DELIVERED) {
                actionButton.setVisibility(View.GONE);
                reviewDisplay.setVisibility(View.GONE);
                return;
            }
            // Delivered + already reviewed → swap the button for the inline review preview.
            if (item.isReviewed() && existingReview != null) {
                actionButton.setVisibility(View.GONE);
                reviewDisplay.setVisibility(View.VISIBLE);
                renderStars(Math.round(existingReview.getRating()));
                String comment = existingReview.getComment();
                if (comment != null && !comment.trim().isEmpty()) {
                    reviewComment.setVisibility(View.VISIBLE);
                    reviewComment.setText("“" + comment + "”");
                } else {
                    reviewComment.setVisibility(View.GONE);
                }
                return;
            }
            // Delivered + not yet reviewed → call to action.
            actionButton.setVisibility(View.VISIBLE);
            reviewDisplay.setVisibility(View.GONE);
            actionButton.setText("Leave a review");
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    itemView.getContext(), R.color.home_balance_background));
            actionButton.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.home_white));
            actionButton.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAction(item, position);
            });
        }

        private void bindSellerAction(OrderLineItem item, ActionListener actionListener, int position) {
            reviewDisplay.setVisibility(View.GONE);
            if (item.getStatus() == Order.Status.DELIVERED) {
                actionButton.setVisibility(View.GONE);
                return;
            }
            actionButton.setVisibility(View.VISIBLE);
            switch (item.getStatus()) {
                case CONFIRMED:  actionButton.setText("Mark as Packed");      break;
                case PACKED:     actionButton.setText("Mark as On the Way"); break;
                case ON_THE_WAY: actionButton.setText("Mark as Delivered");  break;
                default:         actionButton.setVisibility(View.GONE);      return;
            }
            actionButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    itemView.getContext(), R.color.home_balance_background));
            actionButton.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.home_white));
            actionButton.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onAction(item, position);
            });
        }

        private void renderStars(int rating) {
            int filled = ContextCompat.getColor(itemView.getContext(), R.color.review_star_filled);
            int empty  = ContextCompat.getColor(itemView.getContext(), R.color.review_star_empty);
            ImageView[] stars = {star1, star2, star3, star4, star5};
            for (int i = 0; i < stars.length; i++) {
                stars[i].setColorFilter(i < rating ? filled : empty);
            }
        }

        private void bindProgress(Context ctx, Order.Status status) {
            int active = (status == null ? Order.Status.CONFIRMED : status).ordinal() + 1;
            int activeColor   = ContextCompat.getColor(ctx, R.color.home_balance_background);
            int inactiveColor = ContextCompat.getColor(ctx, R.color.home_placeholder);
            View[] segs = {seg1, seg2, seg3, seg4};
            for (int i = 0; i < segs.length; i++) {
                ((GradientDrawable) segs[i].getBackground().mutate())
                        .setColor(i < active ? activeColor : inactiveColor);
            }
        }

        private String buildStatusText(OrderLineItem item) {
            long ts = item.getStatusTimestamp();
            String when = ts > 0
                    ? DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                    : "—";
            switch (item.getStatus() != null ? item.getStatus() : Order.Status.CONFIRMED) {
                case CONFIRMED:  return "Order confirmed " + when;
                case PACKED:     return "Packed " + when;
                case ON_THE_WAY: return "On the way " + when;
                case DELIVERED:  return "Delivered " + when;
                default:         return "";
            }
        }
    }
}
