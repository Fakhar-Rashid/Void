package com.example.avoid;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ValueEventListener;

/**
 * Drop-in chat-icon button with a live unread-count badge.
 *
 * <p>Behavior:
 * <ul>
 *   <li>While attached to the window and the user is logged in, listens to the
 *       buyer-side unread total via {@link ChatRepository#listenForTotalUnreadCount}.</li>
 *   <li>Tap → opens {@link ChatListActivity} (buyer mode), or {@link LoginActivity}
 *       if the user is browsing as a guest.</li>
 *   <li>Re-subscribes on login/logout via the {@link UserSession} listener.</li>
 * </ul>
 *
 * <p>Drop into any layout: {@code <com.example.avoid.ChatIconButton ... />}. No wiring required.
 */
public class ChatIconButton extends FrameLayout {

    private TextView badge;
    private ValueEventListener unreadListener;
    private boolean attached;

    private final Runnable sessionListener = this::resubscribe;

    public ChatIconButton(@NonNull Context context) { this(context, null); }
    public ChatIconButton(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public ChatIconButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_chat_icon, this, true);
        badge = findViewById(R.id.chatIconBadge);
        setClickable(true);
        setFocusable(true);

        // Borderless ripple foreground so the icon feels tappable.
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(
                androidx.appcompat.R.attr.selectableItemBackgroundBorderless, tv, true)) {
            setForeground(getContext().getDrawable(tv.resourceId));
        }

        setOnClickListener(v -> openChats());
    }

    private void openChats() {
        Context ctx = getContext();
        if (!UserSession.getInstance().isLoggedIn()) {
            ctx.startActivity(new Intent(ctx, LoginActivity.class));
            return;
        }
        Intent intent = new Intent(ctx, ChatListActivity.class);
        intent.putExtra("isSellerMode", false);
        ctx.startActivity(intent);
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

        unreadListener = ChatRepository.getInstance().listenForTotalUnreadCount(uid, false,
                new ChatRepository.UnreadCountCallback() {
                    @Override public void onUnreadCountUpdated(int total) {
                        if (!attached) return;
                        if (total > 0) {
                            badge.setVisibility(VISIBLE);
                            badge.setText(total > 99 ? "99+" : String.valueOf(total));
                        } else {
                            badge.setVisibility(GONE);
                        }
                    }
                    @Override public void onError(Exception e) { /* ignore */ }
                });
    }

    private void unsubscribe() {
        if (unreadListener == null) return;
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() != null && session.getCurrentUser().getId() != null) {
            ChatRepository.getInstance().removeTotalUnreadCountListener(
                    session.getCurrentUser().getId(), false, unreadListener);
        }
        unreadListener = null;
    }
}
