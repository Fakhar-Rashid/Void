package com.example.avoid.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.avoid.CartFragment;
import com.example.avoid.FollowedStoresFragment;
import com.example.avoid.HomeFragment;
import com.example.avoid.OrdersFragment;
import com.example.avoid.ProfileFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:  return new OrdersFragment();
            case 2:  return new CartFragment();
            case 3:  return new FollowedStoresFragment();
            case 4:  return new ProfileFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
