package com.sai.decisiongraveyard.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.util.ThemeManager;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DecisionRepository decisionRepository;

    private TextView tvUserEmail;
    private TextView tvTotalDecisions;
    private TextView tvEvaluatedDecisions;
    private TextView tvPendingDecisions;
    private TextView tvMemberSince;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        decisionRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(this).getDecisionRepository();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvTotalDecisions = findViewById(R.id.tvTotalDecisions);
        tvEvaluatedDecisions = findViewById(R.id.tvEvaluatedDecisions);
        tvPendingDecisions = findViewById(R.id.tvPendingDecisions);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        btnLogout = findViewById(R.id.btnLogout);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        loadUserData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }
        tvUserEmail.setText(currentUser.getEmail());
    }

    private void loadUserData() {
        new Thread(() -> {
            try {
                var records = decisionRepository.getAllDecisionRecords();
                int total = records.size();
                int evaluated = 0;
                int pending = 0;

                for (var record : records) {
                    if (record.isEvaluated()) {
                        evaluated++;
                    } else if (!record.isReadyForReview()) {
                        pending++;
                    }
                }

                final int finalTotal = total;
                final int finalEvaluated = evaluated;
                final int finalPending = pending;

                runOnUiThread(() -> {
                    tvTotalDecisions.setText(String.valueOf(finalTotal));
                    tvEvaluatedDecisions.setText(String.valueOf(finalEvaluated));
                    tvPendingDecisions.setText(String.valueOf(finalPending));
                    tvMemberSince.setText("Recently");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
