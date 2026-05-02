package com.example.avoid;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.avoid.model.Color;
import com.example.avoid.model.Condition;
import com.example.avoid.model.Product;
import com.example.avoid.model.ProductCategory;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.example.avoid.model.WeightUnit;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private static final String IMAGE_FOLDER = "product_images";
    private static final int DESCRIPTION_MIN = 250;

    /** Curated palette the seller picks from. Order matters — it's the on-screen layout. */
    private static final List<Color> PALETTE = Arrays.asList(
            new Color("Black",     "#1C1C1E"),
            new Color("White",     "#FFFFFF"),
            new Color("Silver",    "#C0C0C0"),
            new Color("Gold",      "#D4AF37"),
            new Color("Rose Gold", "#B76E79"),
            new Color("Blue",      "#2D7DFF"),
            new Color("Red",       "#E53935"),
            new Color("Green",     "#2E7D32"),
            new Color("Purple",    "#7B3FA0"),
            new Color("Pink",      "#EC407A")
    );

    private TextInputEditText nameInput, priceInput, descInput, weightInput, stockInput;
    private AutoCompleteTextView categoryInput, conditionInput, weightUnitInput;
    private com.google.android.material.textfield.TextInputLayout nameLayout, priceLayout, categoryLayout,
            descriptionLayout, conditionLayout, weightLayout, weightUnitLayout, stockLayout;
    private ViewGroup colorPaletteContainer;
    private ViewGroup imageThumbsContainer;
    private TextView errorText, titleText, imagesErrorText, colorsErrorText;
    private MaterialButton submitButton, deleteButton;
    private View loadingOverlay;
    private TextView loadingText;

    private final List<UriOrUrl> images = new ArrayList<>();
    /** key = hex (uppercased), value = Color (preserves selection order). */
    private final Map<String, Color> selectedColors = new LinkedHashMap<>();

    /** Edit mode: non-null when prefilled from an existing product. */
    private Product editingProduct;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) addLocalImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        findViews();

        ImageButton back = findViewById(R.id.addProductBackButton);
        back.setOnClickListener(v -> finish());

        setupCategoryDropdown();
        setupConditionDropdown();
        setupWeightUnitDropdown();
        setupColorPalette();
        setupDescriptionCounter();

        findViewById(R.id.btnAddImage).setOnClickListener(v -> imagePicker.launch("image/*"));
        submitButton.setOnClickListener(v -> submit());
        deleteButton.setOnClickListener(v -> confirmDelete());

        String editingId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (!TextUtils.isEmpty(editingId)) {
            loadForEdit(editingId);
        }
    }

    private void findViews() {
        nameInput              = findViewById(R.id.inputName);
        priceInput             = findViewById(R.id.inputPrice);
        descInput              = findViewById(R.id.inputDescription);
        weightInput            = findViewById(R.id.inputWeight);
        stockInput             = findViewById(R.id.inputStock);
        categoryInput          = findViewById(R.id.inputCategory);
        conditionInput         = findViewById(R.id.inputCondition);
        weightUnitInput        = findViewById(R.id.inputWeightUnit);
        nameLayout             = findViewById(R.id.inputNameLayout);
        priceLayout            = findViewById(R.id.inputPriceLayout);
        categoryLayout         = findViewById(R.id.inputCategoryLayout);
        descriptionLayout      = findViewById(R.id.inputDescriptionLayout);
        conditionLayout        = findViewById(R.id.inputConditionLayout);
        weightLayout           = findViewById(R.id.inputWeightLayout);
        weightUnitLayout       = findViewById(R.id.inputWeightUnitLayout);
        stockLayout            = findViewById(R.id.inputStockLayout);
        colorPaletteContainer  = findViewById(R.id.colorPaletteContainer);
        imageThumbsContainer   = findViewById(R.id.imageThumbsContainer);
        errorText              = findViewById(R.id.addProductErrorText);
        imagesErrorText        = findViewById(R.id.imagesErrorText);
        colorsErrorText        = findViewById(R.id.colorsErrorText);
        titleText              = findViewById(R.id.addProductTitle);
        submitButton           = findViewById(R.id.btnSubmitProduct);
        deleteButton           = findViewById(R.id.btnDeleteProduct);
        loadingOverlay         = findViewById(R.id.addProductLoading);
        loadingText            = findViewById(R.id.addProductLoadingText);

        wireErrorClearOnEdit();
    }

    /** Clear a field's red error as soon as the user starts editing it. */
    private void wireErrorClearOnEdit() {
        attachClearer(nameInput, nameLayout);
        attachClearer(priceInput, priceLayout);
        attachClearer(descInput, descriptionLayout);
        attachClearer(weightInput, weightLayout);
        attachClearer(stockInput, stockLayout);
        attachClearer(categoryInput, categoryLayout);
        attachClearer(conditionInput, conditionLayout);
        attachClearer(weightUnitInput, weightUnitLayout);
    }

    private void attachClearer(EditText input,
                               com.google.android.material.textfield.TextInputLayout layout) {
        if (input == null || layout == null) return;
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (layout.getError() != null) layout.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupCategoryDropdown() {
        String[] labels = new String[ProductCategory.values().length];
        for (int i = 0; i < labels.length; i++) labels[i] = ProductCategory.values()[i].getDisplayName();
        categoryInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
    }

    private void setupConditionDropdown() {
        String[] labels = new String[Condition.values().length];
        for (int i = 0; i < labels.length; i++) labels[i] = Condition.values()[i].getDisplayName();
        conditionInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
    }

    private void setupWeightUnitDropdown() {
        String[] labels = new String[WeightUnit.values().length];
        for (int i = 0; i < labels.length; i++) labels[i] = WeightUnit.values()[i].name() + " (" + WeightUnit.values()[i].getSymbol() + ")";
        weightUnitInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        weightUnitInput.setText(labels[1], false); // default kg
    }

    private void addLocalImage(@NonNull Uri uri) {
        UriOrUrl entry = new UriOrUrl(uri, null);
        images.add(entry);
        renderThumb(entry);
        if (imagesErrorText != null) imagesErrorText.setVisibility(View.GONE);
    }

    private void addRemoteImage(@NonNull String url) {
        UriOrUrl entry = new UriOrUrl(null, url);
        images.add(entry);
        renderThumb(entry);
    }

    private void renderThumb(UriOrUrl entry) {
        View itemView = getLayoutInflater().inflate(R.layout.item_image_thumb, imageThumbsContainer, false);
        ShapeableImageView img = itemView.findViewById(R.id.imageThumb);
        FrameLayout removeBtn  = itemView.findViewById(R.id.imageThumbRemove);

        Glide.with(this)
                .load(entry.uri != null ? entry.uri : entry.url)
                .placeholder(R.drawable.bg_skeleton)
                .into(img);

        removeBtn.setOnClickListener(v -> {
            images.remove(entry);
            imageThumbsContainer.removeView(itemView);
        });

        imageThumbsContainer.addView(itemView);
    }

    private void setupDescriptionCounter() {
        com.google.android.material.textfield.TextInputLayout layout =
                findViewById(R.id.inputDescriptionLayout);
        if (layout == null) return;
        descInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                int len = s == null ? 0 : s.length();
                if (len < DESCRIPTION_MIN) {
                    layout.setHelperText("Minimum 250 characters · " + (DESCRIPTION_MIN - len) + " to go");
                } else {
                    layout.setHelperText("Looks good · " + len + " characters");
                }
            }
        });
    }

    /**
     * Lay swatches into rows that wrap based on the container's measured width.
     * Container is a vertical LinearLayout; each row inside is a horizontal LinearLayout.
     */
    private void setupColorPalette() {
        colorPaletteContainer.removeAllViews();
        // Defer until the container has a width — we need it to compute swatches-per-row.
        colorPaletteContainer.post(this::layoutPaletteRows);
    }

    private void layoutPaletteRows() {
        colorPaletteContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int swatchSize = (int) (44 * density);
        int marginEnd  = (int) (10 * density);
        int width = colorPaletteContainer.getWidth();
        if (width <= 0) width = (int) (300 * density); // Sane fallback.
        int swatchesPerRow = Math.max(1, (width + marginEnd) / (swatchSize + marginEnd));

        LinearLayout currentRow = null;
        for (int i = 0; i < PALETTE.size(); i++) {
            if (i % swatchesPerRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) rowParams.topMargin = (int) (10 * density);
                currentRow.setLayoutParams(rowParams);
                colorPaletteContainer.addView(currentRow);
            }
            addPaletteSwatch(PALETTE.get(i), currentRow);
        }
    }

    private void addPaletteSwatch(Color color, ViewGroup parent) {
        float density = getResources().getDisplayMetrics().density;
        int swatchSize = (int) (44 * density);
        int marginEnd  = (int) (10 * density);

        FrameLayout container = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(swatchSize, swatchSize);
        params.setMarginEnd(marginEnd);
        container.setLayoutParams(params);
        container.setClickable(true);
        container.setFocusable(true);

        // Swatch fill.
        View fill = new View(this);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        try { shape.setColor(android.graphics.Color.parseColor(color.getHex())); }
        catch (IllegalArgumentException ignore) { shape.setColor(android.graphics.Color.LTGRAY); }
        shape.setStroke((int) density, ContextCompat.getColor(this, R.color.home_chip_border));
        fill.setBackground(shape);
        container.addView(fill, new FrameLayout.LayoutParams(swatchSize, swatchSize));

        // Selection ring + check overlay (created here, toggled by render).
        View ring = new View(this);
        ring.setBackgroundResource(R.drawable.bg_color_swatch_selected);
        container.addView(ring, new FrameLayout.LayoutParams(swatchSize, swatchSize));

        ImageView check = new ImageView(this);
        check.setImageResource(R.drawable.ic_check);
        int checkSize = (int) (20 * density);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(checkSize, checkSize);
        checkParams.gravity = android.view.Gravity.CENTER;
        container.addView(check, checkParams);

        // Tag swatch with its color so we can look it up on click.
        container.setTag(R.id.addProductRoot, color);
        container.setOnClickListener(v -> toggleColor(color, ring, check));

        // Initial selection state.
        renderSwatchSelection(ring, check, color);

        parent.addView(container);
    }

    private void toggleColor(Color color, View ring, ImageView check) {
        String key = keyOf(color);
        if (selectedColors.containsKey(key)) selectedColors.remove(key);
        else selectedColors.put(key, color);
        renderSwatchSelection(ring, check, color);
        if (!selectedColors.isEmpty() && colorsErrorText != null) {
            colorsErrorText.setVisibility(View.GONE);
        }
    }

    private void renderSwatchSelection(View ring, ImageView check, Color color) {
        boolean selected = selectedColors.containsKey(keyOf(color));
        ring.setVisibility(selected ? View.VISIBLE : View.GONE);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
        if (selected) {
            int parsed;
            try { parsed = android.graphics.Color.parseColor(color.getHex()); }
            catch (IllegalArgumentException e) { parsed = android.graphics.Color.LTGRAY; }
            int luminance = (android.graphics.Color.red(parsed)
                    + android.graphics.Color.green(parsed)
                    + android.graphics.Color.blue(parsed)) / 3;
            int tint = luminance > 180 ? android.graphics.Color.BLACK : android.graphics.Color.WHITE;
            check.setColorFilter(tint);
        }
    }

    private void refreshAllSwatches() {
        // setupColorPalette posts the row layout, so this can be called before swatches exist.
        if (colorPaletteContainer.getChildCount() == 0) {
            colorPaletteContainer.post(this::refreshAllSwatches);
            return;
        }
        for (int r = 0; r < colorPaletteContainer.getChildCount(); r++) {
            View rowView = colorPaletteContainer.getChildAt(r);
            if (!(rowView instanceof ViewGroup)) continue;
            ViewGroup row = (ViewGroup) rowView;
            for (int i = 0; i < row.getChildCount(); i++) {
                ViewGroup container = (ViewGroup) row.getChildAt(i);
                Object tag = container.getTag(R.id.addProductRoot);
                if (!(tag instanceof Color)) continue;
                Color color = (Color) tag;
                View ring = container.getChildAt(1);
                ImageView check = (ImageView) container.getChildAt(2);
                renderSwatchSelection(ring, check, color);
            }
        }
    }

    private static String keyOf(Color c) {
        return c.getHex() == null ? "" : c.getHex().toUpperCase();
    }

    private void submit() {
        clearAllErrors();

        boolean ok = true;

        if (images.isEmpty()) {
            showSectionError(imagesErrorText, "Please add at least one image.");
            ok = false;
        }

        String name = textOf(nameInput);
        if (TextUtils.isEmpty(name)) { nameLayout.setError("Required"); ok = false; }

        double price = 0;
        boolean priceOk = false;
        try { price = Double.parseDouble(textOf(priceInput)); priceOk = price > 0; }
        catch (NumberFormatException ignore) {}
        if (!priceOk) { priceLayout.setError("Enter a valid price"); ok = false; }

        ProductCategory category = pickEnum(ProductCategory.values(),
                ProductCategory::getDisplayName, textOf(categoryInput));
        if (category == null) { categoryLayout.setError("Pick a category"); ok = false; }

        String description = textOf(descInput);
        if (description.length() < DESCRIPTION_MIN) {
            descriptionLayout.setError("At least " + DESCRIPTION_MIN + " characters required");
            ok = false;
        }

        Condition condition = pickEnum(Condition.values(),
                Condition::getDisplayName, textOf(conditionInput));
        if (condition == null) { conditionLayout.setError("Pick a condition"); ok = false; }

        double weight = 0;
        boolean weightOk = false;
        try { weight = Double.parseDouble(textOf(weightInput)); weightOk = weight > 0; }
        catch (NumberFormatException ignore) {}
        if (!weightOk) { weightLayout.setError("Enter a valid weight"); ok = false; }

        WeightUnit weightUnit = pickWeightUnit(textOf(weightUnitInput));
        if (weightUnit == null) { weightUnitLayout.setError("Pick a unit"); ok = false; }

        if (selectedColors.isEmpty()) {
            showSectionError(colorsErrorText, "Pick at least one color.");
            ok = false;
        }

        int stock = 0;
        boolean stockOk = false;
        try { stock = Integer.parseInt(textOf(stockInput)); stockOk = stock >= 0; }
        catch (NumberFormatException ignore) {}
        if (!stockOk) { stockLayout.setError("Enter a valid stock"); ok = false; }

        if (!ok) {
            Toast.makeText(this, "Please fill in every field correctly.", Toast.LENGTH_LONG).show();
            errorText.setText("Some fields need attention. Check the highlighted ones above.");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        User user = UserSession.getInstance().getCurrentUser();
        Store store = user != null ? user.getStore() : null;
        if (user == null || store == null) {
            showError(getString(R.string.auth_error_generic));
            return;
        }

        Product product = editingProduct != null ? editingProduct : new Product();
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        product.setCategory(category);
        product.setCondition(condition);
        product.setWeight(weight);
        product.setWeightUnit(weightUnit);
        product.setStock(stock);
        product.setAvailableColors(new ArrayList<>(selectedColors.values()));
        product.setStoreId(store.getOwnerId());
        product.setStoreName(store.getName());
        product.setLocation(store.getLocation());
        if (product.getCreatedAt() == 0) product.setCreatedAt(System.currentTimeMillis());

        uploadAndSave(product);
    }

    private void showSectionError(TextView target, String message) {
        target.setText(message);
        target.setVisibility(View.VISIBLE);
    }

    /** Clears every per-field error and the bottom error summary. Called at the start of submit
     *  and whenever the user adds an image or picks a color, so those errors disappear immediately. */
    private void clearAllErrors() {
        clearError();
        nameLayout.setError(null);
        priceLayout.setError(null);
        categoryLayout.setError(null);
        descriptionLayout.setError(null);
        conditionLayout.setError(null);
        weightLayout.setError(null);
        weightUnitLayout.setError(null);
        stockLayout.setError(null);
        if (imagesErrorText != null) imagesErrorText.setVisibility(View.GONE);
        if (colorsErrorText != null) colorsErrorText.setVisibility(View.GONE);
    }

    private void uploadAndSave(Product product) {
        showLoading(getString(R.string.add_product_uploading));
        uploadAllImages(0, new ArrayList<>(), uploadedUrls -> {
            product.setImageUrls(uploadedUrls);
            showLoading(getString(R.string.add_product_saving));
            ProductRepository.getInstance().saveProduct(product, new ProductRepository.Callback<Product>() {
                @Override public void onSuccess(Product saved) {
                    hideLoading();
                    Toast.makeText(AddProductActivity.this, "Product saved", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override public void onFailure(@NonNull Exception e) {
                    hideLoading();
                    showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                            : getString(R.string.auth_error_generic));
                }
            });
        }, errorMsg -> {
            hideLoading();
            showError(errorMsg);
        });
    }

    private interface UploadDone { void onAll(List<String> urls); }
    private interface UploadFail { void onError(String message); }

    private void uploadAllImages(int index, List<String> accumulated, UploadDone done, UploadFail fail) {
        if (index >= images.size()) {
            done.onAll(accumulated);
            return;
        }
        UriOrUrl entry = images.get(index);
        if (entry.url != null) {
            accumulated.add(entry.url);
            uploadAllImages(index + 1, accumulated, done, fail);
            return;
        }
        ImageUploader.uploadImage(this, entry.uri, IMAGE_FOLDER, new ImageUploader.Callback() {
            @Override public void onSuccess(@NonNull String secureUrl) {
                accumulated.add(secureUrl);
                uploadAllImages(index + 1, accumulated, done, fail);
            }
            @Override public void onFailure(@NonNull String message) {
                fail.onError(message);
            }
        });
    }

    private void loadForEdit(String productId) {
        showLoading(getString(R.string.add_product_saving));
        ProductRepository.getInstance().loadProduct(productId, new ProductRepository.Callback<Product>() {
            @Override public void onSuccess(Product product) {
                hideLoading();
                editingProduct = product;
                titleText.setText(R.string.add_product_title_edit);
                submitButton.setText(R.string.add_product_submit_update);
                deleteButton.setVisibility(View.VISIBLE);
                bindProductForEdit(product);
            }
            @Override public void onFailure(@NonNull Exception e) {
                hideLoading();
                Toast.makeText(AddProductActivity.this, "Failed to load product", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindProductForEdit(Product product) {
        nameInput.setText(product.getName());
        priceInput.setText(String.valueOf(product.getPrice()));
        descInput.setText(product.getDescription());
        if (product.getCategory() != null) categoryInput.setText(product.getCategory().getDisplayName(), false);
        if (product.getCondition() != null) conditionInput.setText(product.getCondition().getDisplayName(), false);
        weightInput.setText(String.valueOf(product.getWeight()));
        if (product.getWeightUnit() != null) {
            weightUnitInput.setText(product.getWeightUnit().name() + " (" + product.getWeightUnit().getSymbol() + ")", false);
        }
        stockInput.setText(String.valueOf(product.getStock()));

        selectedColors.clear();
        for (Color c : product.getAvailableColors()) selectedColors.put(keyOf(c), c);
        refreshAllSwatches();

        for (String url : product.getImageUrls()) addRemoteImage(url);
    }

    private void confirmDelete() {
        if (editingProduct == null || editingProduct.getId() == null) return;
        new AlertDialog.Builder(this)
                .setMessage(R.string.seller_product_delete_confirm)
                .setPositiveButton(R.string.seller_product_action_delete, (d, w) -> deleteProduct())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteProduct() {
        showLoading(getString(R.string.add_product_saving));
        ProductRepository.getInstance().deleteProduct(editingProduct.getId(),
                new ProductRepository.Callback<Void>() {
                    @Override public void onSuccess(Void unused) {
                        hideLoading();
                        Toast.makeText(AddProductActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onFailure(@NonNull Exception e) {
                        hideLoading();
                        showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                                : getString(R.string.auth_error_generic));
                    }
                });
    }

    // ---- helpers ----

    private static String textOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private interface DisplayLabel<E> { String label(E value); }

    private static <E> E pickEnum(E[] values, DisplayLabel<E> labelOf, String text) {
        if (TextUtils.isEmpty(text)) return null;
        for (E v : values) if (labelOf.label(v).equals(text)) return v;
        return null;
    }

    private static WeightUnit pickWeightUnit(String text) {
        if (TextUtils.isEmpty(text)) return null;
        for (WeightUnit u : WeightUnit.values()) {
            String label = u.name() + " (" + u.getSymbol() + ")";
            if (label.equals(text)) return u;
        }
        return null;
    }

    private void showLoading(String message) {
        loadingText.setText(message);
        loadingOverlay.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        errorText.setVisibility(View.GONE);
    }

    /** Wraps either a local Uri (yet to upload) or a remote URL (already uploaded). */
    private static class UriOrUrl {
        final Uri uri;
        final String url;
        UriOrUrl(Uri uri, String url) { this.uri = uri; this.url = url; }
    }

    public static Intent createIntent(@NonNull android.content.Context context, @NonNull String productId) {
        Intent intent = new Intent(context, AddProductActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        return intent;
    }
}
