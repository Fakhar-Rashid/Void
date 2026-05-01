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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;

    private EditText emailInput;
    private EditText passwordInput;
    private TextView errorText;
    private ProgressBar progress;
    private View btnLogin;
    private View btnGoogle;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    setLoading(false);
                    showError(getString(R.string.auth_error_generic));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        emailInput    = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        errorText     = findViewById(R.id.loginErrorText);
        progress      = findViewById(R.id.loginProgress);
        btnLogin      = findViewById(R.id.btnLogin);
        btnGoogle     = findViewById(R.id.btnGoogle);

        ImageButton close = findViewById(R.id.loginCloseButton);
        close.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> attemptEmailLogin());
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        findViewById(R.id.btnGoSignUp).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void attemptEmailLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email)) { showError(getString(R.string.auth_error_email_required)); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showError(getString(R.string.auth_error_email_invalid)); return; }
        if (TextUtils.isEmpty(password)) { showError(getString(R.string.auth_error_password_required)); return; }

        setLoading(true);
        clearError();

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        showError(getString(R.string.auth_error_generic));
                        return;
                    }
                    if (!user.isEmailVerified()) {
                        setLoading(false);
                        auth.signOut();
                        showError(getString(R.string.auth_error_email_not_verified));
                        return;
                    }
                    UserSession.getInstance().loginViaAuth(user, () -> {
                        setLoading(false);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                            : getString(R.string.auth_error_generic));
                });
    }

    private void startGoogleSignIn() {
        setLoading(true);
        clearError();
        googleLauncher.launch(googleClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        showError(getString(R.string.auth_error_generic));
                        return;
                    }
                    UserSession.getInstance().loginViaAuth(user, () -> {
                        setLoading(false);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                            : getString(R.string.auth_error_generic));
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnGoogle.setEnabled(!loading);
    }

    private void showError(String msg) {
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        errorText.setVisibility(View.GONE);
    }
}
