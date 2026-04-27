package com.example.avoid.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.R;
import com.example.avoid.model.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final List<Order> orders;

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {

        private final TextView productName;
        private final TextView orderDate;
        private final TextView colorQty;
        private final TextView price;
        private final View progressContainer;
        private final View labelsContainer;
        private final View deliveredRow;
        private final TextView deliveredDate;
        private final View seg1, seg2, seg3, seg4;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            productName       = itemView.findViewById(R.id.orderProductName);
            orderDate         = itemView.findViewById(R.id.orderDate);
            colorQty          = itemView.findViewById(R.id.orderColorQty);
            price             = itemView.findViewById(R.id.orderPrice);
            progressContainer = itemView.findViewById(R.id.orderProgressContainer);
            labelsContainer   = itemView.findViewById(R.id.orderLabelsContainer);
            deliveredRow      = itemView.findViewById(R.id.orderDeliveredRow);
            deliveredDate     = itemView.findViewById(R.id.orderDeliveredDate);
            seg1              = itemView.findViewById(R.id.orderSeg1);
            seg2              = itemView.findViewById(R.id.orderSeg2);
            seg3              = itemView.findViewById(R.id.orderSeg3);
            seg4              = itemView.findViewById(R.id.orderSeg4);
        }

        void bind(Order order) {
            Context ctx = itemView.getContext();

            com.example.avoid.model.CartProduct first = order.getFirstItem();
            if (first != null) {
                productName.setText(first.getProduct().getName());
                colorQty.setText(first.getColor() + "  ·  Qty: " + first.getQuantity());
                price.setText(first.getProduct().getPrice());
            }
            orderDate.setText(order.getOrderDate());

            boolean delivered = order.getStatus() == Order.Status.DELIVERED;
            progressContainer.setVisibility(delivered ? View.GONE : View.VISIBLE);
            labelsContainer.setVisibility(delivered ? View.GONE : View.VISIBLE);
            deliveredRow.setVisibility(delivered ? View.VISIBLE : View.GONE);

            if (delivered) {
                deliveredDate.setText("Delivered on: " + order.getOrderDate());
            } else {
                bindProgressSegments(ctx, order.getStatus());
            }
        }

        private void bindProgressSegments(Context ctx, Order.Status status) {
            int activeCount = status.ordinal() + 1;
            int activeColor   = ContextCompat.getColor(ctx, R.color.home_balance_background);
            int inactiveColor = ContextCompat.getColor(ctx, R.color.home_placeholder);
            View[] segs = {seg1, seg2, seg3, seg4};
            for (int i = 0; i < segs.length; i++) {
                ((GradientDrawable) segs[i].getBackground().mutate())
                        .setColor(i < activeCount ? activeColor : inactiveColor);
            }
        }
    }
}
