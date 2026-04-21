package com.sai.decisiongraveyard.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.firebase.AuthHelper;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authHelper = new AuthHelper();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authHelper.isUserLoggedIn()) {
            navigateToMain();
        }
    }

    private void registerUser() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText() == null
                ? ""
                : etConfirmPassword.getText().toString().trim();

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
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match.");
            return;
        }

        setLoading(true);
        authHelper.register(email, password, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(@Nullable FirebaseUser user) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Account created successfully.", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        btnLogin.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
