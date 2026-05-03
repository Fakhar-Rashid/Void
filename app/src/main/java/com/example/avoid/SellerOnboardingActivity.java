package com.example.avoid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Seller onboarding / store edit. All branding fields (banner, store logo, owner profile
 * photo) are mandatory — the form refuses to submit until each is provided. In edit mode the
 * existing image URLs are pre-filled and only re-uploaded if the user picks a new file.
 */
public class SellerOnboardingActivity extends AppCompatActivity {

    private static final String FOLDER_BANNERS = "store_banners";
    private static final String FOLDER_LOGOS   = "store_logos";
    private static final String FOLDER_AVATARS = "user_avatars";

    private TextInputEditText nameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextView errorText;
    private MaterialButton submitButton;
    private ProgressBar progress;

    private ShapeableImageView bannerImage, profileImage, logoImage;
    private TextView bannerHint;
    private MaterialButton bannerButton, profileButton, logoButton;

    /** Newly picked URIs (null until the user picks). */
    @Nullable private Uri pendingBannerUri, pendingProfileUri, pendingLogoUri;
    /** Pre-existing remote URLs (carried over in edit mode, or filled from auth photo). */
    @Nullable private String existingBannerUrl, existingProfileUrl, existingLogoUrl;

    private final ActivityResultLauncher<String> bannerPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) onBannerPicked(uri); });
    private final ActivityResultLauncher<String> profilePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) onProfilePicked(uri); });
    private final ActivityResultLauncher<String> logoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) onLogoPicked(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_onboarding);

        nameInput        = findViewById(R.id.onboardingNameInput);
        descriptionInput = findViewById(R.id.onboardingDescriptionInput);
        locationInput    = findViewById(R.id.onboardingLocationInput);
        emailInput       = findViewById(R.id.onboardingEmailInput);
        phoneInput       = findViewById(R.id.onboardingPhoneInput);
        errorText        = findViewById(R.id.onboardingErrorText);
        submitButton     = findViewById(R.id.onboardingSubmitButton);
        progress         = findViewById(R.id.onboardingProgress);

        bannerImage   = findViewById(R.id.onboardingBannerImage);
        bannerHint    = findViewById(R.id.onboardingBannerHint);
        bannerButton  = findViewById(R.id.onboardingBannerButton);
        profileImage  = findViewById(R.id.onboardingProfileImage);
        profileButton = findViewById(R.id.onboardingProfileButton);
        logoImage     = findViewById(R.id.onboardingLogoImage);
        logoButton    = findViewById(R.id.onboardingLogoButton);

        ImageButton back = findViewById(R.id.onboardingBackButton);
        back.setOnClickListener(v -> finish());

        bannerButton.setOnClickListener(v -> bannerPicker.launch("image/*"));
        profileButton.setOnClickListener(v -> profilePicker.launch("image/*"));
        logoButton.setOnClickListener(v -> logoPicker.launch("image/*"));

        User user = UserSession.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            emailInput.setText(user.getEmail());
        }
        if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            existingProfileUrl = user.getProfileImageUrl();
            loadInto(profileImage, existingProfileUrl);
            profileButton.setText(R.string.onboarding_replace_image);
        }

        // Pre-fill the form when an existing store is present so this activity
        // doubles as the seller's "Edit Store" screen.
        if (user != null && user.getStore() != null) {
            Store existing = user.getStore();
            if (existing.getName()         != null) nameInput.setText(existing.getName());
            if (existing.getDescription()  != null) descriptionInput.setText(existing.getDescription());
            if (existing.getLocation()     != null) locationInput.setText(existing.getLocation());
            if (existing.getContactEmail() != null) emailInput.setText(existing.getContactEmail());
            if (existing.getPhone()        != null) phoneInput.setText(existing.getPhone());
            if (existing.getLogoUrl() != null && !existing.getLogoUrl().isEmpty()) {
                existingLogoUrl = existing.getLogoUrl();
                loadInto(logoImage, existingLogoUrl);
                logoButton.setText(R.string.onboarding_replace_image);
            }
            if (existing.getBannerUrl() != null && !existing.getBannerUrl().isEmpty()) {
                existingBannerUrl = existing.getBannerUrl();
                loadInto(bannerImage, existingBannerUrl);
                bannerHint.setVisibility(View.GONE);
                bannerButton.setText(R.string.onboarding_replace_image);
            }
            submitButton.setText(R.string.seller_onboarding_save_changes);
        }

        submitButton.setOnClickListener(v -> submit());
    }

    private void onBannerPicked(@NonNull Uri uri) {
        pendingBannerUri = uri;
        loadInto(bannerImage, uri);
        bannerHint.setVisibility(View.GONE);
        bannerButton.setText(R.string.onboarding_replace_image);
    }

    private void onProfilePicked(@NonNull Uri uri) {
        pendingProfileUri = uri;
        loadInto(profileImage, uri);
        profileButton.setText(R.string.onboarding_replace_image);
    }

    private void onLogoPicked(@NonNull Uri uri) {
        pendingLogoUri = uri;
        loadInto(logoImage, uri);
        logoButton.setText(R.string.onboarding_replace_image);
    }

    private void loadInto(ShapeableImageView target, Object source) {
        Glide.with(this).load(source).centerCrop().into(target);
    }

    private void submit() {
        String name        = textOf(nameInput);
        String description = textOf(descriptionInput);
        String location    = textOf(locationInput);
        String email       = textOf(emailInput);
        String phone       = textOf(phoneInput);

        if (TextUtils.isEmpty(name))     { showError(getString(R.string.onboarding_error_name)); return; }
        if (TextUtils.isEmpty(location)) { showError(getString(R.string.onboarding_error_location)); return; }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.onboarding_error_email));
            return;
        }
        if (pendingProfileUri == null && (existingProfileUrl == null || existingProfileUrl.isEmpty())) {
            showError(getString(R.string.onboarding_error_profile_picture)); return;
        }
        if (pendingLogoUri == null && (existingLogoUrl == null || existingLogoUrl.isEmpty())) {
            showError(getString(R.string.onboarding_error_store_logo)); return;
        }
        if (pendingBannerUri == null && (existingBannerUrl == null || existingBannerUrl.isEmpty())) {
            showError(getString(R.string.onboarding_error_store_banner)); return;
        }

        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || !UserSession.getInstance().isLoggedIn() || user.getId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        clearError();
        setLoading(true);

        // Upload sequentially: profile → logo → banner. Each step skips if the user kept the
        // existing URL. Once all three resolve, write the user + store records.
        uploadIfPicked(pendingProfileUri, existingProfileUrl, FOLDER_AVATARS, profileUrl ->
                uploadIfPicked(pendingLogoUri, existingLogoUrl, FOLDER_LOGOS, logoUrl ->
                        uploadIfPicked(pendingBannerUri, existingBannerUrl, FOLDER_BANNERS, bannerUrl ->
                                persist(user, name, description, location, email, phone,
                                        profileUrl, logoUrl, bannerUrl))));
    }

    private interface UrlReady { void onReady(String url); }

    private void uploadIfPicked(@Nullable Uri picked, @Nullable String existing,
                                @NonNull String folder, @NonNull UrlReady next) {
        if (picked == null) {
            next.onReady(existing);
            return;
        }
        ImageUploader.uploadImage(this, picked, folder, new ImageUploader.Callback() {
            @Override public void onSuccess(@NonNull String secureUrl) { next.onReady(secureUrl); }
            @Override public void onFailure(@NonNull String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private void persist(User user, String name, String description, String location,
                         String email, String phone,
                         String profileUrl, String logoUrl, String bannerUrl) {
        boolean isEdit = user.getStore() != null;
        long createdAt = isEdit ? user.getStore().getCreatedAt() : System.currentTimeMillis();

        Store store = new Store(
                user.getId(),
                name,
                description,
                location,
                email,
                TextUtils.isEmpty(phone) ? null : phone,
                logoUrl,
                bannerUrl,
                createdAt
        );

        // Persist the user's new profile photo first (if any) so the seller flag + photo land
        // together. Then save the store doc and head to the seller activity (or back, in edit).
        if (profileUrl != null && !profileUrl.equals(user.getProfileImageUrl())) {
            user.setProfileImageUrl(profileUrl);
            UserRepository.getInstance().saveProfileImage(user);
        }

        UserRepository.getInstance().saveStore(store, new UserRepository.Callback<Store>() {
            @Override public void onSuccess(Store saved) {
                user.setStore(saved);
                if (!user.isSeller()) {
                    user.setSeller(true);
                    UserRepository.getInstance().saveSellerStatus(user);
                }
                setLoading(false);
                if (isEdit) {
                    finish();
                } else {
                    startActivity(new Intent(SellerOnboardingActivity.this, SellerActivity.class));
                    finish();
                }
            }
            @Override public void onFailure(@NonNull Exception e) {
                setLoading(false);
                showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                        : getString(R.string.auth_error_generic));
            }
        });
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!loading);
        bannerButton.setEnabled(!loading);
        profileButton.setEnabled(!loading);
        logoButton.setEnabled(!loading);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        errorText.setVisibility(View.GONE);
    }
}
