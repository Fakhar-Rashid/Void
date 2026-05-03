package com.example.avoid;

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

import com.example.avoid.model.Store;
import com.example.avoid.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Stores" tab. Shows the stores the current user follows, using the same store card as the
 * one on the product details page.
 */
public class FollowedStoresFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private TextView subtitle;

    private final Runnable sessionListener = this::reload;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_followed_stores, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.followedStoresRecyclerView);
        emptyState   = view.findViewById(R.id.followedStoresEmpty);
        subtitle     = view.findViewById(R.id.followedStoresSubtitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reload();
    }

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
        reload();
    }

    private void reload() {
        if (getView() == null) return;
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null) {
            renderEmpty("Log in to start following stores.");
            return;
        }
        User user = session.getCurrentUser();
        List<String> ids = user.getFollowedStoreIds();
        if (ids.isEmpty()) {
            renderEmpty(null);
            return;
        }
        UserRepository.getInstance().loadStores(ids, new UserRepository.Callback<List<Store>>() {
            @Override public void onSuccess(List<Store> result) {
                if (!isAdded()) return;
                if (result.isEmpty()) {
                    renderEmpty(null);
                    return;
                }
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                subtitle.setText(String.format(Locale.US,
                        "%d store%s you follow", result.size(), result.size() == 1 ? "" : "s"));
                recyclerView.setAdapter(new FollowedStoresAdapter(new ArrayList<>(result)));
            }
            @Override public void onFailure(@NonNull Exception e) {
                if (!isAdded()) return;
                renderEmpty(null);
            }
        });
    }

    private void renderEmpty(@Nullable String overrideHint) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        subtitle.setText("");
        if (overrideHint != null) {
            TextView hint = (TextView) ((ViewGroup) emptyState).getChildAt(2);
            if (hint != null) hint.setText(overrideHint);
        }
    }

    private void openStore(Store store) {
        if (store == null) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, StoreDetailsFragment.newInstance(store))
                .addToBackStack(null)
                .commit();
    }

    private class FollowedStoresAdapter extends RecyclerView.Adapter<FollowedStoresAdapter.VH> {

        private final List<Store> data;

        FollowedStoresAdapter(List<Store> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_store_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Store store = data.get(position);
            StoreCardBinder.bind(holder.itemView, store, FollowedStoresFragment.this::openStore);
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView) { super(itemView); }
        }
    }
}
