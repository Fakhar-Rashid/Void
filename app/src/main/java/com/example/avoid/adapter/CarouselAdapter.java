package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private final List<Integer> imageResIds;

    public CarouselAdapter(List<Integer> imageResIds) {
        this.imageResIds = imageResIds;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        // We use placeholders if imageResIds contains 0 or invalid id, but usually it's set
        if (imageResIds.get(position) != 0) {
            holder.imageView.setImageResource(imageResIds.get(position));
        } else {
            holder.imageView.setBackgroundResource(R.drawable.bg_product_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return imageResIds.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carouselImageView);
        }
    }
}
