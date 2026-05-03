package com.example.avoid;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private View formContainer;
    private View verifyContainer;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private TextView errorText;
    private TextView verifyBody;
    private TextView verifyError;
    private ProgressBar progress;
    private View btnCreate;
    private View btnVerifyContinue;
    private TextView btnVerifyResend;

    private String pendingEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        formContainer    = findViewById(R.id.signupFormContainer);
        verifyContainer  = findViewById(R.id.signupVerifyContainer);
        nameInput        = findViewById(R.id.signupNameInput);
        emailInput       = findViewById(R.id.signupEmailInput);
        passwordInput    = findViewById(R.id.signupPasswordInput);
        errorText        = findViewById(R.id.signupErrorText);
        verifyBody       = findViewById(R.id.verifyBodyText);
        verifyError      = findViewById(R.id.verifyErrorText);
        progress         = findViewById(R.id.signupProgress);
        btnCreate        = findViewById(R.id.btnCreateAccount);
        btnVerifyContinue= findViewById(R.id.btnVerifyContinue);
        btnVerifyResend  = findViewById(R.id.btnVerifyResend);

        ImageButton close = findViewById(R.id.signupCloseButton);
        close.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> attemptSignUp());
        btnVerifyContinue.setOnClickListener(v -> checkVerification());
        btnVerifyResend.setOnClickListener(v -> resendVerification());

        findViewById(R.id.btnGoLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptSignUp() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(name)) { showFormError(getString(R.string.auth_error_name_required)); return; }
        if (TextUtils.isEmpty(email)) { showFormError(getString(R.string.auth_error_email_required)); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showFormError(getString(R.string.auth_error_email_invalid)); return; }
        if (TextUtils.isEmpty(password)) { showFormError(getString(R.string.auth_error_password_required)); return; }
        String passwordError = com.example.avoid.util.PasswordValidator.validate(password);
        if (passwordError != null) { showFormError(passwordError); return; }

        setLoading(true);
        clearFormError();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        showFormError(getString(R.string.auth_error_generic));
                        return;
                    }
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build());
                    user.sendEmailVerification()
                            .addOnSuccessListener(v -> {
                                pendingEmail = email;
                                setLoading(false);
                                showVerifyState(email);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showFormError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                                        : getString(R.string.auth_error_generic));
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showFormError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                            : getString(R.string.auth_error_generic));
                });
    }

    private void checkVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setLoading(true);
        verifyError.setVisibility(View.GONE);
        user.reload().addOnCompleteListener(task -> {
            setLoading(false);
            FirebaseUser refreshed = auth.getCurrentUser();
            if (refreshed != null && refreshed.isEmailVerified()) {
                UserSession.getInstance().loginViaAuth(refreshed, this::finish);
            } else {
                verifyError.setText(R.string.auth_error_email_not_verified);
                verifyError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resendVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        user.sendEmailVerification()
                .addOnSuccessListener(v -> Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        e.getLocalizedMessage() != null ? e.getLocalizedMessage() : getString(R.string.auth_error_generic),
                        Toast.LENGTH_SHORT).show());
    }

    private void showVerifyState(String email) {
        formContainer.setVisibility(View.GONE);
        verifyContainer.setVisibility(View.VISIBLE);
        verifyBody.setText(getString(R.string.auth_verify_body, email));
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!loading);
        btnVerifyContinue.setEnabled(!loading);
    }

    private void showFormError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearFormError() {
        errorText.setVisibility(View.GONE);
    }
}
