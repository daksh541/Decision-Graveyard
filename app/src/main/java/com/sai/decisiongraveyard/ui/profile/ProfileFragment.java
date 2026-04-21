package com.sai.decisiongraveyard.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.util.DateUtils;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private DecisionRepository decisionRepository;
    private UserProfileRepository userProfileRepository;

    private TextView tvUserEmail;
    private TextView tvUserLevel;
    private TextView tvXpProgress;
    private TextView tvTotalDecisions;
    private TextView tvEvaluatedDecisions;
    private TextView tvPendingDecisions;
    private TextView tvActivityCompletionRate;
    private TextView tvDisciplineStreak;
    private TextView tvMemberSince;
    private MaterialButton btnLogout;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        decisionRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();
        userProfileRepository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getUserProfileRepository();

        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserLevel = view.findViewById(R.id.tvUserLevel);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        tvTotalDecisions = view.findViewById(R.id.tvTotalDecisions);
        tvEvaluatedDecisions = view.findViewById(R.id.tvEvaluatedDecisions);
        tvPendingDecisions = view.findViewById(R.id.tvPendingDecisions);
        tvActivityCompletionRate = view.findViewById(R.id.tvActivityCompletionRate);
        tvDisciplineStreak = view.findViewById(R.id.tvDisciplineStreak);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        loadUserData();
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            tvMemberSince.setText(formatMemberSince(currentUser));
        }
        loadUserData();
    }

    private void loadUserData() {
        userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserProfile profile) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    tvUserLevel.setText("Level " + profile.getCurrentLevel() + " • " + profile.getLevelName());
                    tvXpProgress.setText(profile.getCurrentXP() + " / " + profile.getXpToNextLevel() + " XP");
                    tvActivityCompletionRate.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", profile.getActivityCompletionRate()));
                    tvDisciplineStreak.setText(String.valueOf(profile.getDailyDisciplineStreak()));
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    tvUserLevel.setText(getString(R.string.profile_level_unknown));
                    tvXpProgress.setText(getString(R.string.profile_xp_unavailable));
                });
            }
        });

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

                requireActivity().runOnUiThread(() -> {
                    tvTotalDecisions.setText(String.valueOf(finalTotal));
                    tvEvaluatedDecisions.setText(String.valueOf(finalEvaluated));
                    tvPendingDecisions.setText(String.valueOf(finalPending));
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to load stats", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
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
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String formatMemberSince(FirebaseUser currentUser) {
        if (currentUser == null || currentUser.getMetadata() == null) {
            return getString(R.string.profile_member_unknown);
        }

        long createdAt = currentUser.getMetadata().getCreationTimestamp();
        if (createdAt <= 0L) {
            return getString(R.string.profile_member_unknown);
        }

        return DateUtils.formatDateTime(createdAt);
    }
}
