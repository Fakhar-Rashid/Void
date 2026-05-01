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

/**
 * Lightweight placeholder fragment for the seller-side tabs that aren't built out yet.
 * Pass title + body via {@link #newInstance(String, String)}.
 */
public class SellerPlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY  = "body";

    public static SellerPlaceholderFragment newInstance(String title, String body) {
        SellerPlaceholderFragment f = new SellerPlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_BODY, body);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE, "") : "";
        String body  = args != null ? args.getString(ARG_BODY, "") : "";
        ((TextView) view.findViewById(R.id.sellerPlaceholderTitle)).setText(title);
        ((TextView) view.findViewById(R.id.sellerPlaceholderBody)).setText(body);
    }
}
