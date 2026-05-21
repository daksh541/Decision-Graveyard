package com.sai.decisiongraveyard.ui.profile;

import android.app.Activity;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private boolean hasLoadedData;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            auth = FirebaseAuth.getInstance();
            RepositoryProvider provider = RepositoryProvider.getInstance(view.getContext());
            decisionRepository = provider.getDecisionRepository();
            userProfileRepository = provider.getUserProfileRepository();
            userPreferencesRepository = provider.getUserPreferencesRepository();

            bindViews(view);
            setupListeners();
            populateIdentity();
            view.post(this::loadProfileContentIfNeeded);
        } catch (RuntimeException exception) {
            showToast(exception.getMessage() == null ? "Profile failed to open." : exception.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (auth == null) {
            return;
        }
        populateIdentity();
        syncThemeSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hasLoadedData = false;
        tvUserEmail = null;
        tvUserLevel = null;
        tvXpProgress = null;
        tvTotalDecisions = null;
        tvEvaluatedDecisions = null;
        tvPendingDecisions = null;
        tvActivityCompletionRate = null;
        tvDisciplineStreak = null;
        tvMemberSince = null;
        tvNotificationIntensity = null;
        tvReminderValue = null;
        switchDarkTheme = null;
        switchBrutalMode = null;
        switchDailySummary = null;
        switchWeeklyReport = null;
        seekReminderMinutes = null;
        chipGoalFitness = null;
        chipGoalStudy = null;
        chipGoalProductivity = null;
        chipGoalFinance = null;
        btnSavePreferences = null;
        btnLogout = null;
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

        if (seekReminderMinutes != null) {
            seekReminderMinutes.setMax(REMINDER_OPTIONS.length - 1);
        }
        syncThemeSwitch();
    }

    private void populateIdentity() {
        FirebaseUser currentUser = auth == null ? null : auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        if (tvUserEmail != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }
        if (tvMemberSince != null) {
            tvMemberSince.setText(getString(R.string.profile_member_since_label) + " • " + formatMemberSince(currentUser));
        }
    }

    private void loadProfileContentIfNeeded() {
        if (hasLoadedData || !isAdded()) {
            return;
        }
        hasLoadedData = true;
        loadUserData();
        loadPreferences();
    }

    private void setupListeners() {
        if (btnSavePreferences != null) {
            btnSavePreferences.setOnClickListener(v -> savePreferences());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
        if (switchDarkTheme != null) {
            switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (bindingThemeSwitch) {
                    return;
                }
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                ThemeManager.toggleTheme(activity);
                activity.recreate();
            });
        }
        if (switchBrutalMode != null) {
            switchBrutalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (tvNotificationIntensity == null) {
                    return;
                }
                tvNotificationIntensity.setText(isChecked ? "Brutal" : "Normal");
                tvNotificationIntensity.setBackgroundResource(isChecked ? R.drawable.bg_badge_danger : R.drawable.bg_badge_surface);
            });
        }
        if (seekReminderMinutes != null) {
            seekReminderMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (tvReminderValue != null) {
                        tvReminderValue.setText(REMINDER_OPTIONS[progress] + " min");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
    }

    private void syncThemeSwitch() {
        if (switchDarkTheme == null) {
            return;
        }
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
        if (auth == null || auth.getCurrentUser() == null) {
            if (isAdded()) {
                redirectToLogin();
            }
            return;
        }

        try {
            userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
                @Override
                public void onSuccess(com.sai.decisiongraveyard.model.UserProfile profile) {
                    runIfActive(() -> {
                        if (tvUserLevel != null) {
                            tvUserLevel.setText("Level " + profile.getCurrentLevel() + " • " + profile.getLevelName());
                        }
                        if (tvXpProgress != null) {
                            tvXpProgress.setText(profile.getCurrentXP() + " / " + profile.getXpToNextLevel() + " XP");
                        }
                        if (tvActivityCompletionRate != null) {
                            tvActivityCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", profile.getActivityCompletionRate()));
                        }
                        if (tvDisciplineStreak != null) {
                            tvDisciplineStreak.setText(String.valueOf(profile.getDailyDisciplineStreak()));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runIfActive(() -> {
                        if (tvUserLevel != null) {
                            tvUserLevel.setText(getString(R.string.profile_level_unknown));
                        }
                        if (tvXpProgress != null) {
                            tvXpProgress.setText(getString(R.string.profile_xp_unavailable));
                        }
                    });
                }
            });
        } catch (RuntimeException exception) {
            showToast(exception.getMessage() == null ? "Failed to load profile." : exception.getMessage());
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                List<com.sai.decisiongraveyard.model.DecisionRecord> records = decisionRepository.getAllDecisionRecords();
                int total = records.size();
                int evaluated = 0;
                int pending = 0;

                for (com.sai.decisiongraveyard.model.DecisionRecord record : records) {
                    if (record.isEvaluated()) {
                        evaluated++;
                    } else if (!record.isReadyForReview()) {
                        pending++;
                    }
                }

                final int totalCount = total;
                final int evaluatedCount = evaluated;
                final int pendingCount = pending;
                runIfActive(() -> {
                    if (tvTotalDecisions != null) {
                        tvTotalDecisions.setText(String.valueOf(totalCount));
                    }
                    if (tvEvaluatedDecisions != null) {
                        tvEvaluatedDecisions.setText(String.valueOf(evaluatedCount));
                    }
                    if (tvPendingDecisions != null) {
                        tvPendingDecisions.setText(String.valueOf(pendingCount));
                    }
                });
            } catch (Exception exception) {
                showToast("Failed to load stats.");
            }
        });
    }

    private void loadPreferences() {
        if (auth == null || auth.getCurrentUser() == null) {
            return;
        }

        try {
            userPreferencesRepository.getUserPreferences(new UserPreferencesRepository.PreferencesCallback() {
                @Override
                public void onSuccess(com.sai.decisiongraveyard.model.UserPreferences preferences) {
                    runIfActive(() -> {
                        boolean brutalMode = "brutal".equalsIgnoreCase(preferences.getNotificationIntensity());
                        if (switchBrutalMode != null) {
                            switchBrutalMode.setChecked(brutalMode);
                        }
                        if (switchDailySummary != null) {
                            switchDailySummary.setChecked(preferences.isDailySummaryEnabled());
                        }
                        if (switchWeeklyReport != null) {
                            switchWeeklyReport.setChecked(preferences.isWeeklyReportEnabled());
                        }
                        if (tvNotificationIntensity != null) {
                            tvNotificationIntensity.setText(brutalMode ? "Brutal" : "Normal");
                            tvNotificationIntensity.setBackgroundResource(brutalMode ? R.drawable.bg_badge_danger : R.drawable.bg_badge_surface);
                        }

                        int reminderIndex = reminderIndexFor(preferences.getPreMissReminderMinutes());
                        if (seekReminderMinutes != null) {
                            seekReminderMinutes.setProgress(reminderIndex);
                        }
                        if (tvReminderValue != null) {
                            tvReminderValue.setText(REMINDER_OPTIONS[reminderIndex] + " min");
                        }

                        List<String> goals = preferences.getSelectedGoals();
                        if (chipGoalFitness != null) {
                            chipGoalFitness.setChecked(goals != null && goals.contains("fitness"));
                        }
                        if (chipGoalStudy != null) {
                            chipGoalStudy.setChecked(goals != null && goals.contains("study"));
                        }
                        if (chipGoalProductivity != null) {
                            chipGoalProductivity.setChecked(goals != null && goals.contains("productivity"));
                        }
                        if (chipGoalFinance != null) {
                            chipGoalFinance.setChecked(goals != null && goals.contains("finance"));
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    showToast(error == null ? "Failed to load preferences." : error);
                }
            });
        } catch (RuntimeException exception) {
            showToast(exception.getMessage() == null ? "Failed to load preferences." : exception.getMessage());
        }
    }

    private void savePreferences() {
        if (switchBrutalMode == null || switchDailySummary == null || switchWeeklyReport == null || seekReminderMinutes == null) {
            showToast("Profile UI is not ready yet.");
            return;
        }

        if (auth == null || auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        com.sai.decisiongraveyard.model.UserPreferences preferences = new com.sai.decisiongraveyard.model.UserPreferences();
        preferences.setNotificationIntensity(switchBrutalMode.isChecked() ? "brutal" : "normal");
        preferences.setDailySummaryEnabled(switchDailySummary.isChecked());
        preferences.setWeeklyReportEnabled(switchWeeklyReport.isChecked());
        preferences.setPreMissReminderMinutes(REMINDER_OPTIONS[seekReminderMinutes.getProgress()]);
        preferences.setSelectedGoals(getSelectedGoals());

        try {
            userPreferencesRepository.updateUserPreferences(preferences, new UserPreferencesRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    showToast(getString(R.string.profile_preferences_saved));
                }

                @Override
                public void onError(String error) {
                    showToast(error == null ? "Failed to save preferences." : error);
                }
            });
        } catch (RuntimeException exception) {
            showToast(exception.getMessage() == null ? "Failed to save preferences." : exception.getMessage());
        }
    }

    private List<String> getSelectedGoals() {
        List<String> goals = new ArrayList<>();
        if (chipGoalFitness != null && chipGoalFitness.isChecked()) {
            goals.add("fitness");
        }
        if (chipGoalStudy != null && chipGoalStudy.isChecked()) {
            goals.add("study");
        }
        if (chipGoalProductivity != null && chipGoalProductivity.isChecked()) {
            goals.add("productivity");
        }
        if (chipGoalFinance != null && chipGoalFinance.isChecked()) {
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
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(getString(R.string.logout))
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        if (auth != null) {
            auth.signOut();
        }
        redirectToLogin();
    }

    private void redirectToLogin() {
        Activity activity = getActivity();
        if (!isAdded() || activity == null) {
            return;
        }
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        activity.finish();
    }

    private void runIfActive(@NonNull Runnable action) {
        Activity activity = getActivity();
        if (!isAdded() || activity == null) {
            return;
        }
        activity.runOnUiThread(action);
    }

    private void showToast(@NonNull String message) {
        runIfActive(() -> {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        });
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
