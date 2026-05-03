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
        setupStockLabel(view);
        loadStoreInfo(view);
        setupOwnerActions(view);

        ImageButton chatButton = view.findViewById(R.id.footerChatButton);
        chatButton.setOnClickListener(v -> openChat());
    }

    /**
     * Owner-only chrome:
     * <ul>
     *   <li>Show the edit + delete buttons (top-right of the hero).</li>
     *   <li>Hide buyer-only sections that don't apply to your own product — the bottom
     *       footer (chat + add to cart), the Store Information card, and the Choose Color
     *       picker (you set the colours during listing, no need to pick one).</li>
     * </ul>
     */
    private void setupOwnerActions(View root) {
        View ownerActions     = root.findViewById(R.id.productOwnerActions);
        ImageButton editBtn   = root.findViewById(R.id.productEditButton);
        ImageButton deleteBtn = root.findViewById(R.id.productDeleteButton);

        boolean owner = isCurrentUserOwner();

        // Buyer-only sections: hide for the owner.
        toggleVisibility(root, R.id.productDetailsFooter, !owner);
        toggleVisibility(root, R.id.storeInfoLabel,       !owner);
        toggleVisibility(root, R.id.productStoreCard,     !owner);
        toggleVisibility(root, R.id.chooseColorLabel,     !owner);
        toggleVisibility(root, R.id.colorSwatchesLayout,  !owner);

        if (ownerActions == null) return;
        if (!owner) {
            ownerActions.setVisibility(View.GONE);
            return;
        }
        ownerActions.setVisibility(View.VISIBLE);

        editBtn.setOnClickListener(v -> {
            if (!isCurrentUserOwner() || product == null || product.getId() == null) return;
            startActivity(AddProductActivity.createIntent(requireContext(), product.getId()));
        });

        deleteBtn.setOnClickListener(v -> {
            if (!isCurrentUserOwner() || product == null || product.getId() == null) return;
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(product.getName() != null ? product.getName() : "Delete product")
                    .setMessage(R.string.seller_product_delete_confirm)
                    .setPositiveButton(R.string.seller_product_action_delete, (d, w) -> deleteProduct())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private static void toggleVisibility(View root, int id, boolean visible) {
        View v = root.findViewById(id);
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isCurrentUserOwner() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null) return false;
        if (product == null || product.getStoreId() == null) return false;
        return product.getStoreId().equals(session.getCurrentUser().getId());
    }

    private void deleteProduct() {
        if (product == null || product.getId() == null) return;
        ProductRepository.getInstance().deleteProduct(product.getId(),
                new ProductRepository.Callback<Void>() {
                    @Override public void onSuccess(Void unused) {
                        if (!isAdded()) return;
                        android.widget.Toast.makeText(requireContext(), "Deleted",
                                android.widget.Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        if (!isAdded()) return;
                        android.widget.Toast.makeText(requireContext(),
                                e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Delete failed",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openChat() {
        if (!UserSession.getInstance().isLoggedIn()) {
            startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
            return;
        }

        com.example.avoid.model.User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || product == null || product.getStoreId() == null) return;

        if (currentUser.getId().equals(product.getStoreId())) {
            android.widget.Toast.makeText(requireContext(), "You cannot chat with yourself", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.Intent intent = new android.content.Intent(requireContext(), ChatActivity.class);
        intent.putExtra("productId", product.getId());
        intent.putExtra("productName", product.getName());
        intent.putExtra("storeId", product.getStoreId());
        intent.putExtra("storeName", product.getStoreName() != null ? product.getStoreName() : "Store");
        intent.putExtra("buyerId", currentUser.getId());
        intent.putExtra("buyerName", currentUser.getName() != null ? currentUser.getName() : "Buyer");
        
        startActivity(intent);
    }

    private void populateProductDetails(View view) {
        if (product == null) return;

        ((TextView) view.findViewById(R.id.detailProductName)).setText(product.getName());
        ((TextView) view.findViewById(R.id.detailProductPrice)).setText(product.getDisplayPrice());
        ((TextView) view.findViewById(R.id.detailProductRating)).setText(product.getDisplayRating());

        TextView soldCountText = view.findViewById(R.id.detailProductSoldCount);
        if (product.getItemsSold() > 0) {
            soldCountText.setText("| Sold " + formatSold(product.getItemsSold()));
        } else {
            soldCountText.setText("");
        }

        ((TextView) view.findViewById(R.id.detailProductDescription))
                .setText(product.getDescription() != null ? product.getDescription() : "");

        TextView conditionText = view.findViewById(R.id.detailProductCondition);
        conditionText.setText(product.getCondition() != null
                ? product.getCondition().getDisplayName() : "—");

        TextView weightText = view.findViewById(R.id.detailProductWeight);
        weightText.setText(product.getWeight() > 0 ? product.getDisplayWeight() : "—");

        TextView categoryText = view.findViewById(R.id.detailProductCategory);
        categoryText.setText(product.getCategory() != null
                ? product.getCategory().getDisplayName() : "—");

        TextView storefrontText = view.findViewById(R.id.detailProductStorefront);
        storefrontText.setText(product.getStoreName() != null ? product.getStoreName()
                : (product.getLocation() != null ? product.getLocation() : ""));

        bindStoreCard(view);
    }

    /**
     * Loads the full {@link com.example.avoid.model.Store} doc for this product so the shared
     * card view (logo, name, location, follow button) can be wired identically to how it
     * appears in the followed-stores tab.
     */
    private void bindStoreCard(View root) {
        View card = root.findViewById(R.id.productStoreCard);
        if (card == null || product == null || product.getStoreId() == null) return;

        // Optimistic seed from product fields so the card shows something immediately.
        com.example.avoid.model.Store seed = new com.example.avoid.model.Store(
                product.getStoreId(),
                product.getStoreName(),
                null, product.getLocation(), null, null, null, null, 0L);
        StoreCardBinder.bind(card, seed, this::openStoreDetails);

        UserRepository.getInstance().loadStore(product.getStoreId(),
                new UserRepository.Callback<com.example.avoid.model.Store>() {
                    @Override public void onSuccess(com.example.avoid.model.Store store) {
                        if (!isAdded() || store == null) return;
                        if (store.getId() == null) store.setId(product.getStoreId());
                        // Owner id is what the rest of the app uses to identify the store.
                        if (store.getOwnerId() == null) store.setOwnerId(product.getStoreId());
                        StoreCardBinder.bind(card, store, ProductDetailsFragment.this::openStoreDetails);
                    }
                    @Override public void onFailure(@NonNull Exception e) { /* keep seed */ }
                });
    }

    private void openStoreDetails(@NonNull com.example.avoid.model.Store store) {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, StoreDetailsFragment.newInstance(store))
                .addToBackStack(null)
                .commit();
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
        if (product == null) return;
        java.util.List<com.example.avoid.model.Color> colors = product.getAvailableColors();
        if (colors == null || colors.isEmpty()) {
            colorSwatchesLayout.setVisibility(View.GONE);
            return;
        }
        colorSwatchesLayout.setVisibility(View.VISIBLE);

        int size = dpToPx(36);
        if (selectedColorIndex >= colors.size()) selectedColorIndex = 0;

        for (int i = 0; i < colors.size(); i++) {
            com.example.avoid.model.Color color = colors.get(i);
            int parsed;
            try { parsed = Color.parseColor(color.getHex()); }
            catch (IllegalArgumentException e) { parsed = Color.parseColor("#CCCCCC"); }

            boolean isSelected = (i == selectedColorIndex);

            FrameLayout swatchContainer = new FrameLayout(requireContext());
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(size, size);
            containerParams.setMargins(0, 0, dpToPx(12), 0);
            swatchContainer.setLayoutParams(containerParams);

            View colorView = new View(requireContext());
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(parsed);
            colorView.setBackground(shape);
            swatchContainer.addView(colorView, new FrameLayout.LayoutParams(size, size));

            if (isSelected) {
                View ringView = new View(requireContext());
                ringView.setBackgroundResource(R.drawable.bg_color_swatch_selected);
                swatchContainer.addView(ringView, new FrameLayout.LayoutParams(size, size));

                ImageView checkIcon = new ImageView(requireContext());
                checkIcon.setImageResource(R.drawable.ic_check);
                int luminance = (Color.red(parsed) + Color.green(parsed) + Color.blue(parsed)) / 3;
                checkIcon.setImageTintList(ColorStateList.valueOf(luminance > 180 ? Color.BLACK : Color.WHITE));

                int checkSize = dpToPx(18);
                FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(checkSize, checkSize);
                checkParams.gravity = android.view.Gravity.CENTER;
                swatchContainer.addView(checkIcon, checkParams);
            }

            final int position = i;
            swatchContainer.setOnClickListener(v -> {
                selectedColorIndex = position;
                renderColorSwatches();
                updateAddToCartUI();
            });

            colorSwatchesLayout.addView(swatchContainer);
        }
    }

    private void setupReviewsSection(View view) {
        RecyclerView reviewsRecycler = view.findViewById(R.id.reviewsRecyclerView);
        TextView noReviewsLabel = view.findViewById(R.id.noReviewsLabel);

        reviewsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsRecycler.setAdapter(new ReviewAdapter(new ArrayList<>()));

        if (product == null || product.getReviewIds() == null || product.getReviewIds().isEmpty()) {
            reviewsRecycler.setVisibility(View.GONE);
            noReviewsLabel.setVisibility(View.VISIBLE);
            return;
        }

        ReviewRepository.getInstance().loadReviewsByIds(product.getReviewIds(),
                new ReviewRepository.Callback<List<Review>>() {
                    @Override public void onSuccess(List<Review> reviews) {
                        if (getView() == null) return;
                        if (reviews.isEmpty()) {
                            reviewsRecycler.setVisibility(View.GONE);
                            noReviewsLabel.setVisibility(View.VISIBLE);
                        } else {
                            reviewsRecycler.setVisibility(View.VISIBLE);
                            noReviewsLabel.setVisibility(View.GONE);
                            reviewsRecycler.setAdapter(new ReviewAdapter(reviews));
                        }
                    }
                    @Override public void onFailure(@androidx.annotation.NonNull Exception e) {
                        if (getView() == null) return;
                        reviewsRecycler.setVisibility(View.GONE);
                        noReviewsLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadStoreInfo(View view) {
        if (product == null || product.getStoreId() == null) return;
        TextView storefrontText = view.findViewById(R.id.detailProductStorefront);

        UserRepository.getInstance().loadStore(product.getStoreId(),
                new UserRepository.Callback<com.example.avoid.model.Store>() {
                    @Override public void onSuccess(com.example.avoid.model.Store store) {
                        if (getView() == null || store == null) return;
                        String name = store.getName() != null ? store.getName() : "";
                        String loc  = store.getLocation() != null ? store.getLocation() : "";
                        storefrontText.setText(loc.isEmpty() ? name : name + " · " + loc);
                    }
                    @Override public void onFailure(@androidx.annotation.NonNull Exception e) {}
                });
    }

    private void setupAddToCartSection(View view) {
        View addToCartButton = view.findViewById(R.id.footerAddToCartButton);
        View quantityControlLayout = view.findViewById(R.id.quantityControlLayout);
        ImageButton btnMinus = view.findViewById(R.id.btnMinus);
        ImageButton btnPlus = view.findViewById(R.id.btnPlus);
        
        if (product == null) return;

        boolean ownProduct = isOwnedByCurrentUser(product);
        boolean outOfStock = product.isOutOfStock();
        boolean disabled = ownProduct || outOfStock;

        addToCartButton.setEnabled(!disabled);
        addToCartButton.setAlpha(disabled ? 0.4f : 1.0f);

        addToCartButton.setOnClickListener(v -> {
            if (ownProduct) {
                android.widget.Toast.makeText(requireContext(), R.string.self_purchase_blocked,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (outOfStock) {
                android.widget.Toast.makeText(requireContext(), R.string.out_of_stock_label,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedColor = getSelectedColorString();
            com.example.avoid.model.Cart cart = UserSession.getInstance().getCurrentUser().getCart();
            cart.addItem(product.getId(), selectedColor, 1);
            UserRepository.getInstance().saveCartForCurrentUser();
            android.widget.Toast.makeText(requireContext(), "Added to cart",
                    android.widget.Toast.LENGTH_SHORT).show();
            updateAddToCartUI();
        });

        btnPlus.setOnClickListener(v -> {
            String selectedColor = getSelectedColorString();
            com.example.avoid.model.Cart cart = UserSession.getInstance().getCurrentUser().getCart();
            
            int currentQty = 0;
            if (cart != null) {
                for (com.example.avoid.model.CartItem item : cart.getItems()) {
                    if (item.getProductId() != null && item.getProductId().equals(product.getId())
                            && (item.getColor() == null ? selectedColor == null : item.getColor().equals(selectedColor))) {
                        currentQty = item.getQuantity();
                        break;
                    }
                }
            }
            if (currentQty >= product.getStock()) {
                android.widget.Toast.makeText(requireContext(), "Cannot exceed available stock", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            cart.addItem(product.getId(), selectedColor, 1);
            UserRepository.getInstance().saveCartForCurrentUser();
            updateAddToCartUI();
        });

        btnMinus.setOnClickListener(v -> {
            String selectedColor = getSelectedColorString();
            com.example.avoid.model.Cart cart = UserSession.getInstance().getCurrentUser().getCart();
            if (cart != null) {
                for (com.example.avoid.model.CartItem item : cart.getItems()) {
                    if (item.getProductId() != null && item.getProductId().equals(product.getId())
                            && (item.getColor() == null ? selectedColor == null : item.getColor().equals(selectedColor))) {
                        item.setQuantity(item.getQuantity() - 1);
                        if (item.getQuantity() <= 0) {
                            cart.removeItem(item);
                        }
                        UserRepository.getInstance().saveCartForCurrentUser();
                        break;
                    }
                }
            }
            updateAddToCartUI();
        });

        updateAddToCartUI();
    }

    private void updateAddToCartUI() {
        View view = getView();
        if (view == null || product == null) return;
        View addToCartButton = view.findViewById(R.id.footerAddToCartButton);
        View quantityControlLayout = view.findViewById(R.id.quantityControlLayout);
        TextView tvQuantity = view.findViewById(R.id.tvQuantity);

        String selectedColor = getSelectedColorString();
        com.example.avoid.model.Cart cart = UserSession.getInstance().getCurrentUser().getCart();
        int qty = 0;
        if (cart != null) {
            for (com.example.avoid.model.CartItem item : cart.getItems()) {
                if (item.getProductId() != null && item.getProductId().equals(product.getId())
                        && (item.getColor() == null ? selectedColor == null : item.getColor().equals(selectedColor))) {
                    qty = item.getQuantity();
                    break;
                }
            }
        }
        if (qty > 0) {
            addToCartButton.setVisibility(View.GONE);
            quantityControlLayout.setVisibility(View.VISIBLE);
            tvQuantity.setText(String.valueOf(qty));
        } else {
            addToCartButton.setVisibility(View.VISIBLE);
            quantityControlLayout.setVisibility(View.GONE);
        }

        if (getActivity() instanceof CartBadgeUpdater) {
            ((CartBadgeUpdater) getActivity()).updateCartBadge(cart != null ? cart.getTotalItemCount() : 0);
        }
    }

    private void setupStockLabel(View view) {
        TextView stockLabel = view.findViewById(R.id.detailStockLabel);
        if (product == null) { stockLabel.setVisibility(View.GONE); return; }
        if (product.isOutOfStock()) {
            stockLabel.setText(R.string.out_of_stock_label);
            stockLabel.setVisibility(View.VISIBLE);
        } else if (product.isLowStock()) {
            stockLabel.setText(getString(R.string.low_stock_label, product.getStock()));
            stockLabel.setVisibility(View.VISIBLE);
        } else {
            stockLabel.setVisibility(View.GONE);
        }
    }

    private boolean isOwnedByCurrentUser(com.example.avoid.model.Product p) {
        if (p == null || p.getStoreId() == null) return false;
        com.example.avoid.model.User u = UserSession.getInstance().getCurrentUser();
        return UserSession.getInstance().isLoggedIn()
                && u != null
                && p.getStoreId().equals(u.getId());
    }

    private String getSelectedColorString() {
        if (product == null) return null;
        java.util.List<com.example.avoid.model.Color> colors = product.getAvailableColors();
        if (colors == null || colors.isEmpty()) return null;
        int index = Math.min(Math.max(selectedColorIndex, 0), colors.size() - 1);
        return colors.get(index).getName();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static String formatSold(int n) {
        if (n >= 1000) return (n / 100) / 10.0 + "k+";
        if (n >= 100)  return ((n / 100) * 100) + "+";
        return String.valueOf(n);
    }
}
