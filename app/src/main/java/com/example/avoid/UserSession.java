package com.example.avoid;

import androidx.annotation.NonNull;

import com.example.avoid.model.Cart;
import com.example.avoid.model.Settings;
import com.example.avoid.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory session. Always non-null currentUser:
 * - {@link #isLoggedIn()} == false → guest user (in-memory only).
 * - {@link #isLoggedIn()} == true  → real user backed by Firebase Auth + Firestore.
 *
 * On login, the guest cart is merged into the real user's cart (handled by UserRepository).
 * Listeners registered via {@link #addListener(Runnable)} fire on every login/logout.
 */
public class UserSession {

    private static UserSession instance;

    private User currentUser;
    private boolean loggedIn;
    private final List<Runnable> listeners = new ArrayList<>();

    private UserSession() {
        currentUser = createGuest();
        loggedIn = false;
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return loggedIn; }

    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try { r.run(); } catch (Throwable ignored) {}
        }
    }

    /** Hydrate from Firebase Auth on app launch. No-op if no verified user is signed in. */
    public void initFromAuth() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        // Refresh the cached profile so isEmailVerified reflects post-verification state.
        fbUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
            if (refreshed == null) return;
            if (!refreshed.isEmailVerified()) return;
            loginViaAuth(refreshed, null);
        });
    }

    /**
     * Log a Firebase user into the session: fetch profile + cart + orders, merge guest cart, install.
     * Callback (optional) fires when the load completes (success or failure).
     */
    public void loginViaAuth(@NonNull FirebaseUser fbUser, Runnable onComplete) {
        Cart guestCart = !loggedIn && currentUser != null ? currentUser.getCart() : null;

        UserRepository.getInstance().loadOrCreate(fbUser, guestCart,
                new UserRepository.Callback<User>() {
                    @Override public void onSuccess(User user) {
                        currentUser = user;
                        loggedIn = true;
                        notifyListeners();
                        if (onComplete != null) onComplete.run();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        currentUser = createGuest();
        loggedIn = false;
        notifyListeners();
    }

    private static User createGuest() {
        return new User(
                "guest",
                "Guest",
                "",
                null,
                null,
                null,
                0.0,
                new Settings()
        );
    }
}
