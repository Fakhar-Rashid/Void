package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.model.CartProduct;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    private final List<CartProduct> items;
    private final OnCartChangedListener listener;

    public CartAdapter(List<CartProduct> items, OnCartChangedListener listener) {
        this.items = items;
        this.listener = listener;
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
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {

        private final TextView productName;
        private final TextView productColor;
        private final TextView productPrice;
        private final TextView productQty;
        private final TextView btnDecrease;
        private final TextView btnIncrease;
        private final ImageButton btnDelete;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.cartProductName);
            productColor = itemView.findViewById(R.id.cartProductColor);
            productPrice = itemView.findViewById(R.id.cartProductPrice);
            productQty = itemView.findViewById(R.id.cartProductQty);
            btnDecrease = itemView.findViewById(R.id.btnDecreaseQty);
            btnIncrease = itemView.findViewById(R.id.btnIncreaseQty);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
        }

        void bind(CartProduct item) {
            productName.setText(item.getProduct().getName());
            productColor.setText(item.getColor());
            productPrice.setText(item.getProduct().getPrice());
            productQty.setText(String.valueOf(item.getQuantity()));

            btnIncrease.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                CartProduct current = items.get(pos);
                current.setQuantity(current.getQuantity() + 1);
                productQty.setText(String.valueOf(current.getQuantity()));
                listener.onCartChanged();
            });

            btnDecrease.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                CartProduct current = items.get(pos);
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
