package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.ProductAdapter;
import com.example.avoid.adapter.SkeletonAdapter;
import com.example.avoid.model.Product;
import com.example.avoid.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String COLLECTION = "products";

    private RecyclerView bestSellersRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private com.google.firebase.database.ValueEventListener unreadCountListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bestSellersRecyclerView    = view.findViewById(R.id.bestSellersRecyclerView);
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView);

        bestSellersRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        bestSellersRecyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_card_skeleton, 4));
        recommendationsRecyclerView.setAdapter(new SkeletonAdapter(R.layout.item_product_list_skeleton, 4));

        view.findViewById(R.id.searchBar).setOnClickListener(v -> openExplore(null));
        view.findViewById(R.id.messagesButton).setOnClickListener(v -> {
            if (UserSession.getInstance().isLoggedIn()) {
                Intent intent = new Intent(requireContext(), ChatListActivity.class);
                intent.putExtra("isSellerMode", false);
                startActivity(intent);
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        wireCategoryChips(view);
        bindBalance(view);

        loadTopProducts();
        listenForUnreadCount(view);
    }

    private void listenForUnreadCount(View view) {
        if (!UserSession.getInstance().isLoggedIn()) return;
        String userId = UserSession.getInstance().getCurrentUser().getId();
        unreadCountListener = ChatRepository.getInstance().listenForTotalUnreadCount(userId, false, new ChatRepository.UnreadCountCallback() {
            @Override
            public void onUnreadCountUpdated(int totalUnread) {
                TextView badge = view.findViewById(R.id.messagesBadge);
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
    public void onDestroyView() {
        super.onDestroyView();
        if (unreadCountListener != null && UserSession.getInstance().isLoggedIn()) {
            String userId = UserSession.getInstance().getCurrentUser().getId();
            ChatRepository.getInstance().removeTotalUnreadCountListener(userId, false, unreadCountListener);
        }
    }

    private void loadTopProducts() {
        db.collection(COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(8)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        products.add(p);
                    }
                    bestSellersRecyclerView.setAdapter(new ProductAdapter(
                            products, ProductAdapter.LayoutMode.CARD, this::openProductDetails));
                    recommendationsRecyclerView.setAdapter(new ProductAdapter(
                            products, ProductAdapter.LayoutMode.LIST, this::openProductDetails));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load top products", e));
    }

    private void wireCategoryChips(View root) {
        int[] chipIds = {
                R.id.categoryChipLaptop,
                R.id.categoryChipSmartphone,
                R.id.categoryChipMonitor,
                R.id.categoryChipAccessories
        };
        String[] labels = {
                getString(R.string.home_category_laptop),
                getString(R.string.home_category_smartphone),
                getString(R.string.home_category_monitor),
                getString(R.string.home_category_accessories)
        };
        for (int i = 0; i < chipIds.length; i++) {
            View chip = root.findViewById(chipIds[i]);
            if (chip == null) continue;
            final String label = labels[i];
            chip.setOnClickListener(v -> openExplore(label));
        }
    }

    private void bindBalance(View root) {
        UserSession session = UserSession.getInstance();
        User user = session.getCurrentUser();

        TextView amount   = root.findViewById(R.id.balanceAmount);
        View topUpButton  = root.findViewById(R.id.topUpButton);
        View topUpPlus    = root.findViewById(R.id.topUpPlus);
        TextView topUpLabel = root.findViewById(R.id.topUpLabel);

        amount.setText(String.format(Locale.US, "$%.3f", user.getBalance()));

        if (session.isLoggedIn()) {
            topUpPlus.setVisibility(View.VISIBLE);
            topUpLabel.setText(R.string.home_top_up);
            topUpButton.setOnClickListener(null);
        } else {
            topUpPlus.setVisibility(View.GONE);
            topUpLabel.setText(R.string.balance_login_label);
            topUpButton.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), LoginActivity.class)));
        }
    }

    private final Runnable sessionListener = () -> {
        if (getView() != null) bindBalance(getView());
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
        if (getView() != null) bindBalance(getView());
    }

    private void openExplore(@Nullable String initialQuery) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ExploreProductsFragment.newInstance(initialQuery))
                .addToBackStack(null)
                .commit();
    }

    private void openProductDetails(Product product) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, ProductDetailsFragment.newInstance(product))
                .addToBackStack(null)
                .commit();
    }
}
