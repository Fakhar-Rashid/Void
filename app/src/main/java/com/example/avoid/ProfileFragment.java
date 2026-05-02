package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.model.User;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.profileSettingsRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.profileEditProfileRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new EditProfileFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.profileEditAddressRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new EditAddressFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.profileLogoutRow).setOnClickListener(v -> {
            UserSession.getInstance().logout();
            render(view);
        });

        view.findViewById(R.id.profileGuestLoginButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));

        view.findViewById(R.id.profileBecomeSellerButton).setOnClickListener(v -> openSeller());

        render(view);
    }

    private void openSeller() {
        if (!UserSession.getInstance().isLoggedIn()) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }
        User user = UserSession.getInstance().getCurrentUser();
        if (user.getStore() == null) {
            startActivity(new Intent(requireContext(), SellerOnboardingActivity.class));
        } else {
            startActivity(new Intent(requireContext(), SellerActivity.class));
        }
    }

    private final Runnable sessionListener = () -> {
        if (getView() != null) render(getView());
    };

    @Override
    public void onStart() {
        super.onStart();
        UserSession.getInstance().addListener(sessionListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        UserSession.getInstance().removeListener(sessionListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) render(getView());
    }

    private void render(View view) {
        boolean loggedIn = UserSession.getInstance().isLoggedIn();
        view.findViewById(R.id.profileScrollView).setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.profileBottomBar).setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.profileGuestContainer).setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        if (loggedIn) bindUser(view);
    }

    private void bindUser(View view) {
        User user = UserSession.getInstance().getCurrentUser();
        ((TextView) view.findViewById(R.id.profileAvatarInitials)).setText(user.getInitials());
        ((TextView) view.findViewById(R.id.profileName)).setText(user.getName());
        ((TextView) view.findViewById(R.id.profileEmail)).setText(user.getEmail());
        ((TextView) view.findViewById(R.id.balanceAmount))
                .setText(String.format(Locale.US, "$%.3f", user.getBalance()));
    }
}
