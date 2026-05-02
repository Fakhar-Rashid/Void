package com.example.avoid.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.EditProfileFragment;
import com.example.avoid.R;
import com.example.avoid.SellerOnboardingActivity;
import com.example.avoid.SettingsFragment;
import com.example.avoid.UserSession;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;

public class SellerProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.sellerProfileEditStoreRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SellerOnboardingActivity.class)));

        view.findViewById(R.id.sellerProfileEditProfileRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.sellerFragmentContainer, new EditProfileFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.sellerProfileSettingsRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.sellerFragmentContainer, new SettingsFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.sellerProfileLogoutRow).setOnClickListener(v -> {
            UserSession.getInstance().logout();
            requireActivity().finish();
        });

        view.findViewById(R.id.btnSwitchToBuyer).setOnClickListener(v ->
                requireActivity().finish());

        bind(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) bind(getView());
    }

    private void bind(View view) {
        User user = UserSession.getInstance().getCurrentUser();
        Store store = user != null ? user.getStore() : null;

        ((TextView) view.findViewById(R.id.sellerProfileAvatar))
                .setText(initialsFor(store, user));
        ((TextView) view.findViewById(R.id.sellerProfileStoreName))
                .setText(store != null && notBlank(store.getName()) ? store.getName() : "Your Store");

        StringBuilder owner = new StringBuilder();
        if (user != null && notBlank(user.getName())) owner.append("Owned by ").append(user.getName());
        if (user != null && notBlank(user.getEmail())) {
            if (owner.length() > 0) owner.append(" · ");
            owner.append(user.getEmail());
        }
        ((TextView) view.findViewById(R.id.sellerProfileOwner)).setText(owner.toString());
    }

    private static String initialsFor(@Nullable Store store, @Nullable User user) {
        String name = store != null && notBlank(store.getName()) ? store.getName()
                : (user != null ? user.getName() : null);
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
