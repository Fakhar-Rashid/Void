package com.example.avoid;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.avoid.model.NotificationItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * In-app notifications stored at {@code /notifications/{userId}/{pushKey}}.
 *
 * <p>No Firebase Cloud Messaging — the app listens to the RTDB path while open. When it's
 * closed, no system push is delivered (would need FCM for that), but as soon as it reopens the
 * bell shows the count and the list is current.
 *
 * <p>Required RTDB rules (paste under "rules"):
 * <pre>
 * "notifications": {
 *   "$uid": {
 *     ".read":  "auth != null && auth.uid == $uid",
 *     ".write": "auth != null",
 *     ".indexOn": "timestamp"
 *   }
 * }
 * </pre>
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static final String NODE = "notifications";
    private static final int MAX_KEEP = 50;

    private static NotificationRepository instance;
    private final FirebaseDatabase db = FirebaseDatabase.getInstance();

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) instance = new NotificationRepository();
        return instance;
    }

    public interface Listener {
        /** Called whenever the notifications list (or any item's read state) changes. */
        void onChanged(@NonNull List<NotificationItem> all, int unread);
    }

    /** Subscribes to the user's notifications. Caller must {@link #stopListening} when done. */
    @Nullable
    public ValueEventListener listen(@NonNull String userId, @NonNull Listener listener) {
        Query q = db.getReference(NODE).child(userId).orderByChild("timestamp").limitToLast(MAX_KEEP);
        ValueEventListener vel = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                List<NotificationItem> list = new ArrayList<>();
                int unread = 0;
                for (DataSnapshot child : snap.getChildren()) {
                    NotificationItem n = child.getValue(NotificationItem.class);
                    if (n == null) continue;
                    n.setId(child.getKey());
                    list.add(n);
                    if (!n.isRead()) unread++;
                }
                Collections.sort(list, new Comparator<NotificationItem>() {
                    @Override public int compare(NotificationItem a, NotificationItem b) {
                        return Long.compare(b.getTimestamp(), a.getTimestamp());
                    }
                });
                listener.onChanged(list, unread);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listen cancelled for " + userId, error.toException());
            }
        };
        q.addValueEventListener(vel);
        return vel;
    }

    public void stopListening(@NonNull String userId, @Nullable ValueEventListener vel) {
        if (vel == null) return;
        db.getReference(NODE).child(userId).removeEventListener(vel);
    }

    /**
     * Writes a notification under the recipient's node. If the caller is the recipient (e.g. a
     * seller messaging themselves), nothing is written so we don't ping people about their own
     * actions.
     */
    public void send(@NonNull String toUserId, @NonNull NotificationItem n) {
        UserSession session = UserSession.getInstance();
        String currentUid = session.getCurrentUser() != null ? session.getCurrentUser().getId() : null;
        if (currentUid != null && currentUid.equals(toUserId)) return;

        n.setTimestamp(System.currentTimeMillis());
        n.setRead(false);
        DatabaseReference ref = db.getReference(NODE).child(toUserId).push();
        ref.setValue(n)
                .addOnFailureListener(e -> Log.e(TAG, "send failed", e));
    }

    public void markAllAsRead(@NonNull String userId) {
        DatabaseReference ref = db.getReference(NODE).child(userId);
        ref.get().addOnSuccessListener(snap -> {
            for (DataSnapshot child : snap.getChildren()) {
                Boolean read = child.child("read").getValue(Boolean.class);
                if (read != null && !read) child.getRef().child("read").setValue(true);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "markAllAsRead failed", e));
    }

    public void delete(@NonNull String userId, @NonNull String notificationId) {
        db.getReference(NODE).child(userId).child(notificationId).removeValue();
    }
}
