package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Inflates the given skeleton item layout {@code count} times. Used as a placeholder adapter
 * while real data loads; swap in the real adapter once data arrives.
 */
public class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder> {

    private final int layoutRes;
    private final int count;

    public SkeletonAdapter(@LayoutRes int layoutRes, int count) {
        this.layoutRes = layoutRes;
        this.count = count;
    }

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new SkeletonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return count;
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
