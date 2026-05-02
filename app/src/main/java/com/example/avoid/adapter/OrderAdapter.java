package com.example.avoid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.OrderDetailsFragment;
import com.example.avoid.R;
import com.example.avoid.model.Order;

import java.util.List;
import java.util.Locale;

/**
 * Buyer's "My Orders" list. Each row is an order summary card:
 * order id + placement date, item/store count, total, overall status.
 * Tap → {@link OrderDetailsFragment} added on top of MainActivity's fragment_container.
 */
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

        private final TextView orderId;
        private final TextView statusBadge;
        private final TextView date;
        private final TextView summary;
        private final TextView total;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId     = itemView.findViewById(R.id.orderListId);
            statusBadge = itemView.findViewById(R.id.orderListStatusBadge);
            date        = itemView.findViewById(R.id.orderListDate);
            summary     = itemView.findViewById(R.id.orderListSummary);
            total       = itemView.findViewById(R.id.orderListTotal);
        }

        void bind(Order order) {
            String idShort = order.getOrderId() != null && order.getOrderId().length() > 8
                    ? order.getOrderId().substring(0, 8).toUpperCase()
                    : (order.getOrderId() != null ? order.getOrderId() : "—");
            orderId.setText("Order #" + idShort);

            date.setText(order.getOrderDate() != null
                    ? "Placed " + order.getOrderDate()
                    : "Placed —");

            int itemCount = order.getTotalItemCount();
            int storeCount = order.getStoreIds().size();
            String summaryText = itemCount + " item" + (itemCount == 1 ? "" : "s");
            if (storeCount > 1) summaryText += " · " + storeCount + " stores";
            summary.setText(summaryText);

            total.setText(String.format(Locale.US, "$%,.2f", order.getTotalAmount()));

            statusBadge.setText(displayStatus(order));

            itemView.setOnClickListener(v -> {
                if (!(itemView.getContext() instanceof FragmentActivity)) return;
                FragmentActivity activity = (FragmentActivity) itemView.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, OrderDetailsFragment.newInstance(order))
                        .addToBackStack(null)
                        .commit();
            });
        }

        private static String displayStatus(Order order) {
            if (order.isFullyDelivered()) return "Delivered";
            // Mixed-status orders read "Partially shipped" if any items are en route or beyond.
            boolean anyPacked = false, anyOnTheWay = false, anyDelivered = false;
            for (com.example.avoid.model.OrderLineItem item : order.getItems()) {
                Order.Status s = item.getStatus();
                if (s == Order.Status.PACKED) anyPacked = true;
                else if (s == Order.Status.ON_THE_WAY) anyOnTheWay = true;
                else if (s == Order.Status.DELIVERED) anyDelivered = true;
            }
            if (anyDelivered)  return "Partially delivered";
            if (anyOnTheWay)   return "On the way";
            if (anyPacked)     return "Packed";
            return "Confirmed";
        }
    }
}
