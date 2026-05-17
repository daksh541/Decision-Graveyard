package com.sai.decisiongraveyard.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.MainActivity;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.firebase.AuthHelper;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    public static final String EXTRA_START_MODE = "start_mode";
    private static final String MODE_CREATE = "create";

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnPrimaryAuth;
    private MaterialButton btnAuthSecondary;
    private ProgressBar progressBar;
    private AuthHelper authHelper;
    private GoogleSignInClient googleSignInClient;
    private TextView tvForgotPassword;
    private TextView tvAuthHeading;
    private TextView tvAuthCaption;
    private MaterialButtonToggleGroup authModeToggle;
    private boolean isCreateMode;

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
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnPrimaryAuth = findViewById(R.id.btnPrimaryAuth);
        btnAuthSecondary = findViewById(R.id.btnAuthSecondary);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvAuthHeading = findViewById(R.id.tvAuthHeading);
        tvAuthCaption = findViewById(R.id.tvAuthCaption);
        authModeToggle = findViewById(R.id.authModeToggle);

        authModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            isCreateMode = checkedId == R.id.btnModeCreate;
            updateModeUi();
        });

        btnPrimaryAuth.setOnClickListener(v -> {
            if (isCreateMode) {
                registerUser();
            } else {
                loginUser();
            }
        });
        btnAuthSecondary.setOnClickListener(v -> {
            authModeToggle.check(isCreateMode ? R.id.btnModeSignIn : R.id.btnModeCreate);
        });
        tvForgotPassword.setOnClickListener(v -> resetPassword());
        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signInWithGoogle());

        isCreateMode = MODE_CREATE.equals(getIntent().getStringExtra(EXTRA_START_MODE));
        authModeToggle.check(isCreateMode ? R.id.btnModeCreate : R.id.btnModeSignIn);
        updateModeUi();
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
        tilConfirmPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();

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
                navigateToMain();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerUser() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText() == null
                ? ""
                : etConfirmPassword.getText().toString();

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
                Toast.makeText(
                        LoginActivity.this,
                        "If this email has password sign-in enabled, a reset email will arrive shortly.",
                        Toast.LENGTH_LONG
                ).show();
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
        etConfirmPassword.setEnabled(!isLoading);
        btnPrimaryAuth.setEnabled(!isLoading);
        btnAuthSecondary.setEnabled(!isLoading);
        authModeToggle.setEnabled(!isLoading);
        findViewById(R.id.btnGoogleSignIn).setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
    }

    private void updateModeUi() {
        tilConfirmPassword.setVisibility(isCreateMode ? View.VISIBLE : View.GONE);
        tvForgotPassword.setVisibility(isCreateMode ? View.GONE : View.VISIBLE);

        if (isCreateMode) {
            tvAuthHeading.setText("Create your discipline profile.");
            tvAuthCaption.setText("Set up your account, calibrate your baseline, and start logging the decisions that shape your score.");
            btnPrimaryAuth.setText("Create Account");
            btnAuthSecondary.setText("Already have an account?");
        } else {
            tvAuthHeading.setText("Take Control.");
            tvAuthCaption.setText("Track patterns, analyze choices, and bury bad habits permanently.");
            btnPrimaryAuth.setText("Continue");
            btnAuthSecondary.setText("Need an account?");
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
