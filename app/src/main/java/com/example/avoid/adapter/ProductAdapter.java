package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.avoid.R;
import com.example.avoid.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public enum LayoutMode {
        CARD(R.layout.item_product_card),
        LIST(R.layout.item_product_list),
        LIST_HORIZONTAL(R.layout.item_product_list),
        /** Column-filling card for 2-column staggered grids (e.g. search results). */
        GRID(R.layout.item_product_grid);

        private final int layoutResId;

        LayoutMode(int layoutResId) {
            this.layoutResId = layoutResId;
        }

        public int getLayoutResId() {
            return layoutResId;
        }
    }

    private final List<Product> products;
    private final LayoutMode layoutMode;
    private final OnProductClickListener listener;

    public ProductAdapter(List<Product> products, LayoutMode layoutMode, OnProductClickListener listener) {
        this.products = products;
        this.layoutMode = layoutMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutMode.getLayoutResId(), parent, false);
        if (layoutMode == LayoutMode.LIST_HORIZONTAL) {
            int widthPx = (int) (290 * parent.getContext().getResources().getDisplayMetrics().density);
            view.setLayoutParams(new ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ImageView productImageView;
        private final TextView productNameTextView;
        private final TextView productPriceTextView;
        private final TextView productLocationTextView;
        private final TextView productRatingTextView;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView        = itemView.findViewById(R.id.productImageView);
            productNameTextView     = itemView.findViewById(R.id.productNameTextView);
            productPriceTextView    = itemView.findViewById(R.id.productPriceTextView);
            productLocationTextView = itemView.findViewById(R.id.productLocationTextView);
            productRatingTextView   = itemView.findViewById(R.id.productRatingTextView);
        }

        void bind(Product product) {
            productNameTextView.setText(product.getName());
            productPriceTextView.setText(product.getDisplayPrice());
            productLocationTextView.setText(product.getLocation());
            productRatingTextView.setText(product.getDisplayRatingSummary());

            if (productImageView != null) {
                Glide.with(productImageView.getContext())
                        .load(product.getMainImageUrl())
                        .placeholder(R.drawable.bg_product_placeholder)
                        .error(R.drawable.bg_product_placeholder)
                        .into(productImageView);
            }
        }
    }
}
