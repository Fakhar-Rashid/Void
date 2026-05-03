package com.example.avoid;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.example.avoid.model.NotificationItem;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * Drop-in bell-icon button with a live unread-count badge.
 *
 * <p>Subscribes to {@link NotificationRepository} while attached to the window. Tap opens
 * {@link NotificationsFragment} on the host {@link FragmentActivity} — the host must expose a
 * fragment container at one of: {@code R.id.fragment_container} (buyer MainActivity) or
 * {@code R.id.sellerFragmentContainer} (SellerActivity). Falls back gracefully if neither is
 * present (does nothing on tap).
 */
public class NotificationBellButton extends FrameLayout {

    private TextView badge;
    @Nullable private ValueEventListener subscription;
    private boolean attached;

    private final Runnable sessionListener = this::resubscribe;

    public NotificationBellButton(@NonNull Context context) { this(context, null); }
    public NotificationBellButton(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public NotificationBellButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_notification_bell, this, true);
        badge = findViewById(R.id.notificationBellBadge);
        setClickable(true);
        setFocusable(true);

        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(
                androidx.appcompat.R.attr.selectableItemBackgroundBorderless, tv, true)) {
            setForeground(getContext().getDrawable(tv.resourceId));
        }

        setOnClickListener(v -> openNotifications());
    }

    private void openNotifications() {
        Context ctx = getContext();
        if (!UserSession.getInstance().isLoggedIn()) {
            ctx.startActivity(new android.content.Intent(ctx, LoginActivity.class));
            return;
        }
        if (!(ctx instanceof FragmentActivity)) return;
        FragmentActivity activity = (FragmentActivity) ctx;
        int containerId = pickContainerId(activity);
        if (containerId == 0) return;

        activity.getSupportFragmentManager().beginTransaction()
                .add(containerId, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }

    private int pickContainerId(FragmentActivity activity) {
        if (activity.findViewById(R.id.fragment_container) != null) return R.id.fragment_container;
        if (activity.findViewById(R.id.sellerFragmentContainer) != null) return R.id.sellerFragmentContainer;
        return 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        UserSession.getInstance().addListener(sessionListener);
        subscribe();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
        UserSession.getInstance().removeListener(sessionListener);
        unsubscribe();
    }

    private void resubscribe() {
        if (!attached) return;
        unsubscribe();
        subscribe();
    }

    private void subscribe() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null) {
            badge.setVisibility(GONE);
            return;
        }
        String uid = session.getCurrentUser().getId();
        if (uid == null) return;

        subscription = NotificationRepository.getInstance().listen(uid,
                new NotificationRepository.Listener() {
                    @Override
                    public void onChanged(@NonNull List<NotificationItem> all, int unread) {
                        if (!attached) return;
                        if (unread > 0) {
                            badge.setVisibility(VISIBLE);
                            badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                        } else {
                            badge.setVisibility(GONE);
                        }
                    }
                });
    }

    private void unsubscribe() {
        if (subscription == null) return;
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() != null && session.getCurrentUser().getId() != null) {
            NotificationRepository.getInstance().stopListening(
                    session.getCurrentUser().getId(), subscription);
        }
        subscription = null;
    }
}
