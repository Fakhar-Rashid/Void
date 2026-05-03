package com.example.avoid;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Empty frame layout that doubles as a fragment container — the well-known id
        // R.id.fragment_container lets NotificationBellButton add the notifications screen
        // on top of the chat list when the bell is tapped.
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(R.id.fragment_container);
        setContentView(frameLayout);
        
        setupSystemBars();

        if (savedInstanceState == null) {
            boolean isSellerMode = getIntent().getBooleanExtra("isSellerMode", false);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ChatListFragment.newInstance(isSellerMode))
                    .commit();
        }
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
