package com.example.avoid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.avoid.adapter.OrderAdapter;
import com.example.avoid.model.CartProduct;
import com.example.avoid.model.Order;
import com.example.avoid.model.Product;

import java.util.Arrays;
import java.util.List;

public class OrdersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.ordersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new OrderAdapter(buildSeedOrders()));

        return view;
    }

    private List<Order> buildSeedOrders() {
        return Arrays.asList(
                new Order("VD-00481",
                        new CartProduct(new Product("Sony WH-1000XM5", "$349.99", "Tokyo, JP", "4.9 (2.1k)"), "Midnight Black", 1),
                        Order.Status.DELIVERED, "Apr 10, 2026"),

                new Order("VD-00476",
                        new CartProduct(new Product("MacBook Pro M3", "$1,999.00", "Cupertino, CA", "4.8 (987)"), "Space Gray", 1),
                        Order.Status.ON_THE_WAY, "Apr 14, 2026"),

                new Order("VD-00469",
                        new CartProduct(new Product("Nike Air Max 270", "$129.99", "Portland, OR", "4.7 (3.4k)"), "Triple White", 2),
                        Order.Status.PACKED, "Apr 15, 2026"),

                new Order("VD-00463",
                        new CartProduct(new Product("iPad Pro 12.9\"", "$1,099.00", "Cupertino, CA", "4.9 (1.2k)"), "Silver", 1),
                        Order.Status.CONFIRMED, "Apr 16, 2026"),

                new Order("VD-00455",
                        new CartProduct(new Product("Samsung Galaxy S24 Ultra", "$1,299.00", "Seoul, KR", "4.8 (4.5k)"), "Titanium Gray", 1),
                        Order.Status.DELIVERED, "Apr 2, 2026"),

                new Order("VD-00448",
                        new CartProduct(new Product("Keychron Q1 Pro", "$199.00", "Hong Kong", "4.9 (876)"), "Carbon Black", 1),
                        Order.Status.CONFIRMED, "Apr 16, 2026"),

                new Order("VD-00441",
                        new CartProduct(new Product("Canon EOS R50", "$679.99", "Tokyo, JP", "4.7 (654)"), "White", 1),
                        Order.Status.ON_THE_WAY, "Apr 13, 2026"),

                new Order("VD-00434",
                        new CartProduct(new Product("Dyson V15 Detect", "$749.99", "London, UK", "4.8 (1.8k)"), "Yellow/Nickel", 1),
                        Order.Status.PACKED, "Apr 15, 2026")
        );
    }
}
