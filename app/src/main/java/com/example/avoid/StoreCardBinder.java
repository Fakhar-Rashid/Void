package com.example.avoid;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Binds a {@code @layout/view_store_card} root to a {@link Store}, including the live
 * Follow ⇄ Following toggle. Used in product details and the followed-stores list so the
 * card looks and behaves identically in both places.
 */
public final class StoreCardBinder {

    public interface OnCardClick {
        void onCardClick(@NonNull Store store);
    }

    private StoreCardBinder() {}

    public static void bind(@NonNull View cardRoot, @NonNull Store store, @NonNull OnCardClick onClick) {
        ShapeableImageView logo  = cardRoot.findViewById(R.id.storeCardLogo);
        TextView name            = cardRoot.findViewById(R.id.storeCardName);
        TextView location        = cardRoot.findViewById(R.id.storeCardLocation);
        MaterialButton followBtn = cardRoot.findViewById(R.id.storeCardFollowButton);

        name.setText(store.getName() != null ? store.getName() : "Store");
        location.setText(store.getLocation() != null ? store.getLocation() : "");
        if (store.getLogoUrl() != null && !store.getLogoUrl().isEmpty()) {
            Glide.with(cardRoot).load(store.getLogoUrl()).centerCrop().into(logo);
        } else {
            logo.setImageResource(R.drawable.ic_profile);
        }

        cardRoot.setOnClickListener(v -> onClick.onCardClick(store));

        if (isOwnStore(store)) {
            followBtn.setVisibility(View.GONE);
        } else {
            followBtn.setVisibility(View.VISIBLE);
            renderFollowState(followBtn, isFollowing(store));
            followBtn.setOnClickListener(v -> toggleFollow(followBtn, store));
        }
    }

    private static boolean isOwnStore(@NonNull Store store) {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) return false;
        User user = session.getCurrentUser();
        return user != null && store.getOwnerId() != null
                && store.getOwnerId().equals(user.getId());
    }

    private static boolean isFollowing(@NonNull Store store) {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) return false;
        User user = session.getCurrentUser();
        return user != null && user.isFollowing(store.getOwnerId());
    }

    private static void toggleFollow(@NonNull MaterialButton button, @NonNull Store store) {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            button.getContext().startActivity(
                    new android.content.Intent(button.getContext(), LoginActivity.class));
            return;
        }
        User user = session.getCurrentUser();
        if (user == null || store.getOwnerId() == null) return;

        boolean targetState = !user.isFollowing(store.getOwnerId());
        // Optimistic UI: flip immediately, the persist is fire-and-forget.
        renderFollowState(button, targetState);
        UserRepository.getInstance().setFollowing(user, store.getOwnerId(), targetState, null);
    }

    private static void renderFollowState(@NonNull MaterialButton button, boolean following) {
        if (following) {
            button.setText("Following");
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(button.getContext(), R.color.home_balance_background)));
            button.setTextColor(ContextCompat.getColor(button.getContext(), R.color.home_white));
            button.setStrokeWidth(0);
        } else {
            button.setText("Follow");
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(button.getContext(), android.R.color.transparent)));
            button.setTextColor(ContextCompat.getColor(button.getContext(), R.color.home_text_primary));
            button.setStrokeWidth((int) (button.getResources().getDisplayMetrics().density));
        }
    }
}
