package com.example.avoid;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.avoid.adapter.ViewPagerAdapter;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity implements CartBadgeUpdater {

    private static final int CART_TAB_INDEX = 2;

    private static final int[] TAB_ICONS = {
            R.drawable.ic_home,
            R.drawable.ic_orders,
            R.drawable.ic_bag,
            R.drawable.ic_profile
    };

    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        UserSession.getInstance().initFromAuth();

        setupSystemBars();
        applyWindowInsets();
        setupTabs();
    }

    private void setupSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.home_background));
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), findViewById(R.id.main));
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setIcon(TAB_ICONS[position])
        ).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        });
    }

    @Override
    public void updateCartBadge(int count) {
        TabLayout.Tab cartTab = tabLayout.getTabAt(CART_TAB_INDEX);
        if (cartTab == null) return;

        BadgeDrawable badge = cartTab.getOrCreateBadge();
        badge.setBackgroundColor(ContextCompat.getColor(this, R.color.home_balance_background));
        badge.setBadgeTextColor(Color.WHITE);

        if (count > 0) {
            badge.setNumber(count);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }
}
