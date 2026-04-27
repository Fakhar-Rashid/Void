package com.example.avoid;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.avoid.adapter.CarouselAdapter;
import com.example.avoid.adapter.ReviewAdapter;
import com.example.avoid.model.Product;
import com.example.avoid.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsFragment extends Fragment {

    private static final String ARG_PRODUCT = "product";
    private Product product;
    private boolean isDescriptionExpanded = false;

    private LinearLayout colorSwatchesLayout;
    private final int[] swatchColors = {
            Color.parseColor("#1C1C1E"), // Black
            Color.parseColor("#E5E5EA"), // Silver
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#007AFF")  // Blue
    };
    private int selectedColorIndex = 0;

    public static ProductDetailsFragment newInstance(Product product) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCT, product);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            product = (Product) getArguments().getSerializable(ARG_PRODUCT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        populateProductDetails(view);
        setupImageCarousel(view);
        setupDescriptionToggle(view);
        setupColorSwatches(view);
        setupReviewsSection(view);
        setupAddToCartSection(view);
    }

    private void populateProductDetails(View view) {
        if (product == null) return;

        TextView nameText = view.findViewById(R.id.detailProductName);
        TextView priceText = view.findViewById(R.id.detailProductPrice);
        TextView ratingText = view.findViewById(R.id.detailProductRating);
        TextView soldCountText = view.findViewById(R.id.detailProductSoldCount);
        TextView storefrontText = view.findViewById(R.id.detailProductStorefront);

        nameText.setText(product.getName());
        priceText.setText(product.getPrice());
        
        // Extract rating and sold count from "4.8 | Sold 250+"
        String ratingSummary = product.getRatingSummary();
        if (ratingSummary != null && ratingSummary.contains("|")) {
            String[] parts = ratingSummary.split("\\|");
            ratingText.setText(parts[0].trim());
            soldCountText.setText("| " + parts[1].trim());
        } else {
            ratingText.setText(ratingSummary != null ? ratingSummary : "0.0");
            soldCountText.setText("");
        }

        storefrontText.setText(product.getLocation());
    }

    private void setupImageCarousel(View view) {
        ViewPager2 viewPager = view.findViewById(R.id.productImagesViewPager);
        LinearLayout dotsLayout = view.findViewById(R.id.dotIndicatorsLayout);

        List<String> imageUrls = (product != null && product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                ? product.getImageUrls()
                : new ArrayList<>();
        CarouselAdapter adapter = new CarouselAdapter(imageUrls);
        viewPager.setAdapter(adapter);

        ImageView[] dots = new ImageView[imageUrls.size()];
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(requireContext());
            dots[i].setImageResource(R.drawable.bg_avatar_circle); // Reusing a simple shape
            int size = dpToPx(8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dots[i].setLayoutParams(params);
            dots[i].setImageTintList(ColorStateList.valueOf(Color.parseColor("#66FFFFFF")));
            dotsLayout.addView(dots[i]);
        }
        
        if (dots.length > 0) {
            dots[0].setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageTintList(ColorStateList.valueOf(
                            i == position ? Color.WHITE : Color.parseColor("#66FFFFFF")));
                }
            }
        });
    }

    private void setupDescriptionToggle(View view) {
        TextView descriptionText = view.findViewById(R.id.detailProductDescription);
        TextView toggleText = view.findViewById(R.id.toggleDescriptionButton);

        toggleText.setOnClickListener(v -> {
            if (isDescriptionExpanded) {
                descriptionText.setMaxLines(3);
                toggleText.setText("More info");
            } else {
                descriptionText.setMaxLines(Integer.MAX_VALUE);
                toggleText.setText("Less info");
            }
            isDescriptionExpanded = !isDescriptionExpanded;
        });
    }

    private void setupColorSwatches(View view) {
        colorSwatchesLayout = view.findViewById(R.id.colorSwatchesLayout);
        renderColorSwatches();
    }

    private void renderColorSwatches() {
        colorSwatchesLayout.removeAllViews();
        int size = dpToPx(36);

        for (int i = 0; i < swatchColors.length; i++) {
            int color = swatchColors[i];
            boolean isSelected = (i == selectedColorIndex);

            FrameLayout swatchContainer = new FrameLayout(requireContext());
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(size, size);
            containerParams.setMargins(0, 0, dpToPx(12), 0);
            swatchContainer.setLayoutParams(containerParams);

            // The color circle
            View colorView = new View(requireContext());
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(color);
            colorView.setBackground(shape);
            swatchContainer.addView(colorView, new FrameLayout.LayoutParams(size, size));

            // Selection ring and checkmark
            if (isSelected) {
                View ringView = new View(requireContext());
                ringView.setBackgroundResource(R.drawable.bg_color_swatch_selected);
                swatchContainer.addView(ringView, new FrameLayout.LayoutParams(size, size));

                ImageView checkIcon = new ImageView(requireContext());
                checkIcon.setImageResource(R.drawable.ic_check);
                // Invert checkmark color if the background is light
                if (color == Color.parseColor("#E5E5EA")) {
                    checkIcon.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                } else {
                    checkIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                }
                
                int checkSize = dpToPx(18);
                FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(checkSize, checkSize);
                checkParams.gravity = android.view.Gravity.CENTER;
                swatchContainer.addView(checkIcon, checkParams);
            }

            final int position = i;
            swatchContainer.setOnClickListener(v -> {
                selectedColorIndex = position;
                renderColorSwatches();
            });

            colorSwatchesLayout.addView(swatchContainer);
        }
    }

    private void setupReviewsSection(View view) {
        RecyclerView reviewsRecycler = view.findViewById(R.id.reviewsRecyclerView);
        reviewsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        List<Review> dummyReviews = new ArrayList<>();
        dummyReviews.add(new Review("Alex Johnson", 5.0f, "Today", "Amazing product! Exceeded all my expectations."));
        dummyReviews.add(new Review("Maria Lopez", 4.5f, "Yesterday", "Very good quality, arrived quickly."));
        dummyReviews.add(new Review("Sam Smith", 4.0f, "2 days ago", "Works exactly as described. Happy with my purchase."));
        dummyReviews.add(new Review("Emily Wong", 5.0f, "Last week", "Best purchase ever! Highly recommend to everyone."));
        dummyReviews.add(new Review("Michael Brown", 4.0f, "Last week", "It is pretty decent, no major issues so far."));

        ReviewAdapter adapter = new ReviewAdapter(dummyReviews);
        reviewsRecycler.setAdapter(adapter);
    }

    private void setupAddToCartSection(View view) {
        View addToCartButton = view.findViewById(R.id.footerAddToCartButton);
        addToCartButton.setOnClickListener(v -> {
            if (product != null) {
                String selectedColor = getSelectedColorString();
                CartManager.getInstance().addProduct(product, selectedColor, 1);
                android.widget.Toast.makeText(requireContext(), "Added to cart", android.widget.Toast.LENGTH_SHORT).show();
                
                if (getActivity() instanceof CartBadgeUpdater) {
                    ((CartBadgeUpdater) getActivity()).updateCartBadge(CartManager.getInstance().getTotalItemCount());
                }
            }
        });
    }

    private String getSelectedColorString() {
        switch (selectedColorIndex) {
            case 0: return "Black";
            case 1: return "Silver";
            case 2: return "Gold";
            case 3: return "Blue";
            default: return "Unknown";
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
