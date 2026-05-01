package com.example.avoid;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.avoid.seller.SellerHomeFragment;
import com.example.avoid.seller.SellerPlaceholderFragment;
import com.example.avoid.seller.SellerProfileFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

public class SellerActivity extends AppCompatActivity {

    /** Bottom-bar tabs that map to a fragment in the container (excludes the centre + button). */
    private static final List<Integer> SWITCHABLE_TABS = Arrays.asList(
            R.id.sellerTabHome,
            R.id.sellerTabOrders,
            R.id.sellerTabChat,
            R.id.sellerTabProfile
    );

    @IdRes private int currentTabId = R.id.sellerTabHome;
    private com.google.firebase.database.ValueEventListener unreadCountListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UserSession.getInstance().getCurrentUser().getStore() == null) {
            startActivity(new android.content.Intent(this, SellerOnboardingActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seller);

        setupSystemBars();
        applyWindowInsets();
        wireTabs();

        if (savedInstanceState == null) {
            selectTab(R.id.sellerTabHome);
        }
        
        listenForUnreadCount();
    }

    private void listenForUnreadCount() {
        String userId = UserSession.getInstance().getCurrentUser().getId();
        unreadCountListener = ChatRepository.getInstance().listenForTotalUnreadCount(userId, true, new ChatRepository.UnreadCountCallback() {
            @Override
            public void onUnreadCountUpdated(int totalUnread) {
                TextView badge = findViewById(R.id.sellerChatBadge);
                if (badge != null) {
                    if (totalUnread > 0) {
                        badge.setVisibility(View.VISIBLE);
                        badge.setText(totalUnread > 99 ? "99+" : String.valueOf(totalUnread));
                    } else {
                        badge.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadCountListener != null) {
            String userId = UserSession.getInstance().getCurrentUser().getId();
            ChatRepository.getInstance().removeTotalUnreadCountListener(userId, true, unreadCountListener);
        }
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(R.id.sellerRoot));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sellerRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });
    }

    private void wireTabs() {
        for (int id : SWITCHABLE_TABS) {
            findViewById(id).setOnClickListener(v -> selectTab(v.getId()));
        }
        findViewById(R.id.sellerFab).setOnClickListener(v -> openAddProduct());
    }

    private void selectTab(@IdRes int tabId) {
        currentTabId = tabId;
        Fragment fragment = fragmentForTab(tabId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sellerFragmentContainer, fragment)
                .commit();
        updateTabTints();
        updateFabVisibility();
    }

    private void updateFabVisibility() {
        FloatingActionButton fab = findViewById(R.id.sellerFab);
        if (fab == null) return;
        if (currentTabId == R.id.sellerTabProfile) fab.hide();
        else fab.show();
    }

    private void openAddProduct() {
        startActivity(new android.content.Intent(this, AddProductActivity.class));
    }

    private Fragment fragmentForTab(@IdRes int tabId) {
        if (tabId == R.id.sellerTabHome) {
            return new SellerHomeFragment();
        } else if (tabId == R.id.sellerTabOrders) {
            return new com.example.avoid.seller.SellerOrdersFragment();
        } else if (tabId == R.id.sellerTabChat) {
            return ChatListFragment.newInstance(true);
        } else if (tabId == R.id.sellerTabProfile) {
            return new SellerProfileFragment();
        }
        return SellerPlaceholderFragment.newInstance("", "");
    }

    private void updateTabTints() {
        int active   = ContextCompat.getColor(this, R.color.home_white);
        int inactive = ContextCompat.getColor(this, R.color.home_nav_inactive);
        for (int id : SWITCHABLE_TABS) {
            View tab = findViewById(id);
            if (tab == null) continue;
            View child = ((android.view.ViewGroup) tab).getChildAt(0);
            ImageView icon;
            if (child instanceof android.widget.FrameLayout) {
                icon = (ImageView) ((android.view.ViewGroup) child).getChildAt(0);
            } else {
                icon = (ImageView) child;
            }
            icon.setColorFilter(id == currentTabId ? active : inactive);
        }
    }
}
