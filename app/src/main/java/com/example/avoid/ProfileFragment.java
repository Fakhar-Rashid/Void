package com.example.avoid;

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
        bindUser(view);

        view.findViewById(R.id.profileSettingsRow).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit());
    }

    private void bindUser(View view) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;

        ((TextView) view.findViewById(R.id.profileAvatarInitials)).setText(user.getInitials());
        ((TextView) view.findViewById(R.id.profileName)).setText(user.getName());
        ((TextView) view.findViewById(R.id.profileEmail)).setText(user.getEmail());
        ((TextView) view.findViewById(R.id.balanceAmount))
                .setText(String.format(Locale.US, "$%.3f", user.getBalance()));
    }
}
