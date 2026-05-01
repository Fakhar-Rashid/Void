package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.OrderAdapter;
import com.example.avoid.model.Order;

import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private View guestContainer;
    private View emptyContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView   = view.findViewById(R.id.ordersRecyclerView);
        guestContainer = view.findViewById(R.id.ordersGuestContainer);
        emptyContainer = view.findViewById(R.id.ordersEmptyContainer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.ordersGuestLoginButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));

        view.findViewById(R.id.ordersExploreButton).setOnClickListener(v -> openExplore());

        render();
    }

    private final Runnable sessionListener = this::render;

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
        render();
    }

    private void render() {
        boolean loggedIn = UserSession.getInstance().isLoggedIn();

        if (!loggedIn) {
            guestContainer.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            recyclerView.setAdapter(null);
            return;
        }

        List<Order> orders = UserSession.getInstance().getCurrentUser().getOrders();
        if (orders.isEmpty()) {
            guestContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            recyclerView.setAdapter(null);
        } else {
            guestContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new OrderAdapter(orders));
        }
    }

    private void openExplore() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ExploreProductsFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }
}
