package com.example.avoid.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.avoid.R;
import com.example.avoid.UserSession;
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

        User user = UserSession.getInstance().getCurrentUser();
        ((TextView) view.findViewById(R.id.sellerProfileAvatar)).setText(user.getInitials());
        ((TextView) view.findViewById(R.id.sellerProfileName)).setText(user.getName());
        ((TextView) view.findViewById(R.id.sellerProfileEmail)).setText(user.getEmail());

        view.findViewById(R.id.btnSwitchToBuyer).setOnClickListener(v ->
                requireActivity().finish());
    }
}
