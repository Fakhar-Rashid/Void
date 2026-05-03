package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

/**
 * Launch screen. Four slot-machine characters cycle through random glyphs while the app
 * loads; when both the minimum display time has elapsed AND the user record (if signed in)
 * is fetched, the slots lock in sequence to spell V·O·I·D, then the screen hands off to
 * {@link MainActivity}.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_DISPLAY_MS    = 2500L;
    private static final long SCRAMBLE_TICK_MS  = 70L;
    private static final long LOCK_INTERVAL_MS  = 220L;
    private static final long POST_LOCK_PAUSE_MS = 350L;

    private static final char[] FINAL = {'V', 'O', 'I', 'D'};
    private static final char[] POOL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#@%&*?".toCharArray();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private TextView[] slots;
    private final boolean[] locked = new boolean[FINAL.length];
    private boolean minTimeReached = false;
    private boolean dataReady = false;
    private boolean lockSequenceStarted = false;
    private boolean navigated = false;

    private final Runnable scrambleTick = new Runnable() {
        @Override public void run() {
            for (int i = 0; i < slots.length; i++) {
                if (locked[i]) continue;
                slots[i].setText(String.valueOf(POOL[random.nextInt(POOL.length)]));
            }
            handler.postDelayed(this, SCRAMBLE_TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        slots = new TextView[]{
                findViewById(R.id.splashChar0),
                findViewById(R.id.splashChar1),
                findViewById(R.id.splashChar2),
                findViewById(R.id.splashChar3)
        };

        // Begin scrambling almost immediately — the four asterisks linger just long enough
        // to register as "loading dots" before the slot machine kicks in.
        handler.postDelayed(scrambleTick, 250L);

        handler.postDelayed(() -> { minTimeReached = true; tryStartLockSequence(); }, MIN_DISPLAY_MS);

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            dataReady = true;
            return;
        }
        fbUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
            if (refreshed == null || !refreshed.isEmailVerified()) {
                dataReady = true;
                tryStartLockSequence();
                return;
            }
            UserSession.getInstance().loginViaAuth(refreshed, () -> {
                dataReady = true;
                tryStartLockSequence();
            });
        });
    }

    private void tryStartLockSequence() {
        if (lockSequenceStarted || !minTimeReached || !dataReady) return;
        lockSequenceStarted = true;
        lockSlot(0);
    }

    /** Recursively locks slot {@code index} to the final letter, then schedules the next. */
    private void lockSlot(int index) {
        if (index >= FINAL.length) {
            handler.postDelayed(this::navigate, POST_LOCK_PAUSE_MS);
            return;
        }
        locked[index] = true;
        slots[index].setText(String.valueOf(FINAL[index]));
        handler.postDelayed(() -> lockSlot(index + 1), LOCK_INTERVAL_MS);
    }

    private void navigate() {
        if (navigated) return;
        navigated = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
