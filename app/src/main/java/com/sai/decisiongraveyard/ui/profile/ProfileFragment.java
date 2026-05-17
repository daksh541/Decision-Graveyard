package com.sai.decisiongraveyard.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.repository.UserPreferencesRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;
import com.sai.decisiongraveyard.util.DateUtils;
import com.sai.decisiongraveyard.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final int[] REMINDER_OPTIONS = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60};

    private FirebaseAuth auth;
    private DecisionRepository decisionRepository;
    private UserProfileRepository userProfileRepository;
    private UserPreferencesRepository userPreferencesRepository;

    private TextView tvUserEmail;
    private TextView tvUserLevel;
    private TextView tvXpProgress;
    private TextView tvTotalDecisions;
    private TextView tvEvaluatedDecisions;
    private TextView tvPendingDecisions;
    private TextView tvActivityCompletionRate;
    private TextView tvDisciplineStreak;
    private TextView tvMemberSince;
    private TextView tvNotificationIntensity;
    private TextView tvReminderValue;
    private SwitchMaterial switchDarkTheme;
    private SwitchMaterial switchBrutalMode;
    private SwitchMaterial switchDailySummary;
    private SwitchMaterial switchWeeklyReport;
    private SeekBar seekReminderMinutes;
    private Chip chipGoalFitness;
    private Chip chipGoalStudy;
    private Chip chipGoalProductivity;
    private Chip chipGoalFinance;
    private MaterialButton btnSavePreferences;
    private MaterialButton btnLogout;

    private boolean bindingThemeSwitch;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        RepositoryProvider provider = RepositoryProvider.getInstance(requireContext());
        decisionRepository = provider.getDecisionRepository();
        userProfileRepository = provider.getUserProfileRepository();
        userPreferencesRepository = provider.getUserPreferencesRepository();

        bindViews(view);
        setupListeners();
        loadUserData();
        loadPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            tvMemberSince.setText(getString(R.string.profile_member_since_label) + " • " + formatMemberSince(currentUser));
        }
        syncThemeSwitch();
        loadUserData();
    }

    private void bindViews(android.view.View view) {
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserLevel = view.findViewById(R.id.tvUserLevel);
        tvXpProgress = view.findViewById(R.id.tvXpProgress);
        tvTotalDecisions = view.findViewById(R.id.tvTotalDecisions);
        tvEvaluatedDecisions = view.findViewById(R.id.tvEvaluatedDecisions);
        tvPendingDecisions = view.findViewById(R.id.tvPendingDecisions);
        tvActivityCompletionRate = view.findViewById(R.id.tvActivityCompletionRate);
        tvDisciplineStreak = view.findViewById(R.id.tvDisciplineStreak);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        tvNotificationIntensity = view.findViewById(R.id.tvNotificationIntensity);
        tvReminderValue = view.findViewById(R.id.tvReminderValue);
        switchDarkTheme = view.findViewById(R.id.switchDarkTheme);
        switchBrutalMode = view.findViewById(R.id.switchBrutalMode);
        switchDailySummary = view.findViewById(R.id.switchDailySummary);
        switchWeeklyReport = view.findViewById(R.id.switchWeeklyReport);
        seekReminderMinutes = view.findViewById(R.id.seekReminderMinutes);
        chipGoalFitness = view.findViewById(R.id.chipGoalFitness);
        chipGoalStudy = view.findViewById(R.id.chipGoalStudy);
        chipGoalProductivity = view.findViewById(R.id.chipGoalProductivity);
        chipGoalFinance = view.findViewById(R.id.chipGoalFinance);
        btnSavePreferences = view.findViewById(R.id.btnSavePreferences);
        btnLogout = view.findViewById(R.id.btnLogout);

        seekReminderMinutes.setMax(REMINDER_OPTIONS.length - 1);
        syncThemeSwitch();
    }

    private void setupListeners() {
        btnSavePreferences.setOnClickListener(v -> savePreferences());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingThemeSwitch) {
                return;
            }
            ThemeManager.toggleTheme(requireContext());
            requireActivity().recreate();
        });
        switchBrutalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvNotificationIntensity.setText(isChecked ? "Brutal" : "Normal");
            tvNotificationIntensity.setBackgroundResource(isChecked ? R.drawable.bg_badge_danger : R.drawable.bg_badge_surface);
        });
        seekReminderMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvReminderValue.setText(REMINDER_OPTIONS[progress] + " min");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void syncThemeSwitch() {
        bindingThemeSwitch = true;
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        boolean darkEnabled = nightMode == AppCompatDelegate.MODE_NIGHT_YES
                || (nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                && (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        switchDarkTheme.setChecked(darkEnabled);
        bindingThemeSwitch = false;
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
                    tvActivityCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", profile.getActivityCompletionRate()));
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
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load stats", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void loadPreferences() {
        userPreferencesRepository.getUserPreferences(new UserPreferencesRepository.PreferencesCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserPreferences preferences) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    boolean brutalMode = "brutal".equalsIgnoreCase(preferences.getNotificationIntensity());
                    switchBrutalMode.setChecked(brutalMode);
                    switchDailySummary.setChecked(preferences.isDailySummaryEnabled());
                    switchWeeklyReport.setChecked(preferences.isWeeklyReportEnabled());
                    tvNotificationIntensity.setText(brutalMode ? "Brutal" : "Normal");
                    tvNotificationIntensity.setBackgroundResource(brutalMode ? R.drawable.bg_badge_danger : R.drawable.bg_badge_surface);

                    int reminderIndex = reminderIndexFor(preferences.getPreMissReminderMinutes());
                    seekReminderMinutes.setProgress(reminderIndex);
                    tvReminderValue.setText(REMINDER_OPTIONS[reminderIndex] + " min");

                    List<String> goals = preferences.getSelectedGoals();
                    chipGoalFitness.setChecked(goals != null && goals.contains("fitness"));
                    chipGoalStudy.setChecked(goals != null && goals.contains("study"));
                    chipGoalProductivity.setChecked(goals != null && goals.contains("productivity"));
                    chipGoalFinance.setChecked(goals != null && goals.contains("finance"));
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void savePreferences() {
        com.sai.decisiongraveyard.model.UserPreferences preferences = new com.sai.decisiongraveyard.model.UserPreferences();
        preferences.setNotificationIntensity(switchBrutalMode.isChecked() ? "brutal" : "normal");
        preferences.setDailySummaryEnabled(switchDailySummary.isChecked());
        preferences.setWeeklyReportEnabled(switchWeeklyReport.isChecked());
        preferences.setPreMissReminderMinutes(REMINDER_OPTIONS[seekReminderMinutes.getProgress()]);
        preferences.setSelectedGoals(getSelectedGoals());

        userPreferencesRepository.updateUserPreferences(preferences, new UserPreferencesRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), getString(R.string.profile_preferences_saved), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private List<String> getSelectedGoals() {
        List<String> goals = new ArrayList<>();
        if (chipGoalFitness.isChecked()) {
            goals.add("fitness");
        }
        if (chipGoalStudy.isChecked()) {
            goals.add("study");
        }
        if (chipGoalProductivity.isChecked()) {
            goals.add("productivity");
        }
        if (chipGoalFinance.isChecked()) {
            goals.add("finance");
        }
        return goals;
    }

    private int reminderIndexFor(int minutes) {
        for (int i = 0; i < REMINDER_OPTIONS.length; i++) {
            if (REMINDER_OPTIONS[i] == minutes) {
                return i;
            }
        }
        return 1;
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout))
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> performLogout())
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
