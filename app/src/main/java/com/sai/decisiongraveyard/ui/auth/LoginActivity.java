package com.sai.decisiongraveyard.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.firebase.AuthHelper;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private AuthHelper authHelper;
    private GoogleSignInClient googleSignInClient;
    private android.widget.TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authHelper = new AuthHelper();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> resetPassword());
        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signInWithGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authHelper.isUserLoggedIn()) {
            navigateToMain();
        }
    }

    private void loginUser() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address.");
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required.");
            return;
        }

        setLoading(true);
        authHelper.login(email, password, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(@Nullable FirebaseUser user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetPassword() {
        tilEmail.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required to reset password.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address.");
            return;
        }

        setLoading(true);
        authHelper.resetPassword(email, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(@Nullable FirebaseUser user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                authHelper.signInWithGoogle(account, new AuthHelper.AuthCallback() {
                    @Override
                    public void onSuccess(@Nullable FirebaseUser user) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Google sign-in successful.", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (ApiException e) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        btnLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        findViewById(R.id.btnGoogleSignIn).setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
