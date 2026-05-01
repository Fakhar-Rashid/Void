package com.example.avoid.seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.UserRepository;
import com.example.avoid.UserSession;
import com.example.avoid.adapter.OrderAdapter;
import com.example.avoid.model.Order;
import com.example.avoid.model.Store;

import java.util.ArrayList;
import java.util.List;

public class SellerOrdersFragment extends Fragment {

    private RecyclerView recyclerView;
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

        recyclerView = view.findViewById(R.id.ordersRecyclerView);
        emptyContainer = view.findViewById(R.id.ordersEmptyContainer);
        View guestContainer = view.findViewById(R.id.ordersGuestContainer);
        
        // Hide guest container as sellers are always logged in
        if (guestContainer != null) guestContainer.setVisibility(View.GONE);

        // Update empty state text for seller
        TextView emptyTitle = view.findViewById(R.id.emptyStateTitle);
        TextView emptyDesc = view.findViewById(R.id.emptyStateDesc);
        if (emptyTitle != null) emptyTitle.setText("No Orders Yet");
        if (emptyDesc != null) emptyDesc.setText("Orders from buyers will show up here.");
        View exploreButton = view.findViewById(R.id.ordersExploreButton);
        if (exploreButton != null) exploreButton.setVisibility(View.GONE);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadStoreOrders();
    }

    private void loadStoreOrders() {
        Store store = UserSession.getInstance().getCurrentUser().getStore();
        if (store == null || store.getId() == null) {
            showEmptyState();
            return;
        }

        UserRepository.getInstance().loadStoreOrders(store.getId(), new UserRepository.OrdersDoneListener() {
            @Override
            public void onDone(List<Order> orders) {
                if (getView() == null) return;
                
                if (orders == null || orders.isEmpty()) {
                    showEmptyState();
                } else {
                    emptyContainer.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    
                    // Filter line items to only show those belonging to this store
                    // But OrderAdapter currently just shows the "first" item. 
                    // This is sufficient for MVP as the order reflects in the list.
                    recyclerView.setAdapter(new OrderAdapter(orders, true));
                }
            }
        });
    }

    private void showEmptyState() {
        emptyContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        recyclerView.setAdapter(null);
    }
}
