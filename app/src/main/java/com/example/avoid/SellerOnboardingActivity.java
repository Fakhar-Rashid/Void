package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.avoid.model.Store;
import com.example.avoid.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SellerOnboardingActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextView errorText;
    private MaterialButton submitButton;
    private ProgressBar progress;

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

        ImageButton back = findViewById(R.id.onboardingBackButton);
        back.setOnClickListener(v -> finish());

        User user = UserSession.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            emailInput.setText(user.getEmail());
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
            submitButton.setText(R.string.seller_onboarding_save_changes);
        }

        submitButton.setOnClickListener(v -> submit());
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

        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || !UserSession.getInstance().isLoggedIn() || user.getId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        clearError();
        setLoading(true);

        boolean isEdit = user.getStore() != null;
        long createdAt = isEdit ? user.getStore().getCreatedAt() : System.currentTimeMillis();
        String existingLogo = isEdit ? user.getStore().getLogoUrl() : null;

        Store store = new Store(
                user.getId(),
                name,
                description,
                location,
                email,
                TextUtils.isEmpty(phone) ? null : phone,
                existingLogo,
                createdAt
        );

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
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        errorText.setVisibility(View.GONE);
    }
}
