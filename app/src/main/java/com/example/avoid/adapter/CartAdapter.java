package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.avoid.R;
import com.example.avoid.model.CartItem;
import com.example.avoid.model.Product;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(CartItem item, Product product);
    }

    private final List<CartItem> items;
    private final Map<String, Product> productsById;
    private final OnCartChangedListener listener;
    private OnItemClickListener itemClickListener;

    public CartAdapter(List<CartItem> items, Map<String, Product> productsById,
                       OnCartChangedListener listener) {
        this.items = items;
        this.productsById = productsById;
        this.listener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_product, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        Product product = productsById.get(item.getProductId());
        holder.bind(item, product);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && product != null) itemClickListener.onItemClick(item, product);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView productImage;
        private final TextView productName;
        private final TextView productColor;
        private final TextView productPrice;
        private final TextView productQty;
        private final TextView btnDecrease;
        private final TextView btnIncrease;
        private final ImageButton btnDelete;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.cartProductImage);
            productName  = itemView.findViewById(R.id.cartProductName);
            productColor = itemView.findViewById(R.id.cartProductColor);
            productPrice = itemView.findViewById(R.id.cartProductPrice);
            productQty   = itemView.findViewById(R.id.cartProductQty);
            btnDecrease  = itemView.findViewById(R.id.btnDecreaseQty);
            btnIncrease  = itemView.findViewById(R.id.btnIncreaseQty);
            btnDelete    = itemView.findViewById(R.id.btnDeleteProduct);
        }

        void bind(CartItem item, Product product) {
            if (product == null) {
                productName.setText("Product unavailable");
                productColor.setText("");
                productPrice.setText("");
                productQty.setText(String.valueOf(item.getQuantity()));
                productImage.setImageDrawable(null);
                btnIncrease.setEnabled(false);
                btnDecrease.setEnabled(false);
            } else {
                productName.setText(product.getName());
                productColor.setText(item.getColor() != null ? item.getColor() : "");
                productPrice.setText(product.getDisplayPrice());
                productQty.setText(String.valueOf(item.getQuantity()));

                Glide.with(productImage.getContext())
                        .load(product.getMainImageUrl())
                        .placeholder(R.drawable.bg_product_placeholder)
                        .error(R.drawable.bg_product_placeholder)
                        .into(productImage);

                int maxStock = product.getStock();
                btnIncrease.setEnabled(item.getQuantity() < maxStock);
                btnDecrease.setEnabled(true);
            }

            btnIncrease.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                CartItem current = items.get(pos);
                int max = product != null ? product.getStock() : Integer.MAX_VALUE;
                if (current.getQuantity() < max) {
                    current.setQuantity(current.getQuantity() + 1);
                    productQty.setText(String.valueOf(current.getQuantity()));
                    listener.onCartChanged();
                }
            });

            btnDecrease.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                CartItem current = items.get(pos);
                if (current.getQuantity() > 1) {
                    current.setQuantity(current.getQuantity() - 1);
                    productQty.setText(String.valueOf(current.getQuantity()));
                    listener.onCartChanged();
                }
            });

            btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                items.remove(pos);
                notifyItemRemoved(pos);
                listener.onCartChanged();
            });
        }
    }
}
