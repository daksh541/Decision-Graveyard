package com.sai.decisiongraveyard.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.UserPreferences;
import com.sai.decisiongraveyard.repository.UserPreferencesRepository;
import com.sai.decisiongraveyard.ui.auth.LoginActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private FirebaseAuth auth;
    private UserPreferencesRepository repository;
    private CheckBox cbGoalFitness;
    private CheckBox cbGoalStudy;
    private CheckBox cbGoalProductivity;
    private CheckBox cbGoalFinance;
    private RadioGroup rgNotificationIntensity;
    private RadioButton rbNormal;
    private RadioButton rbBrutal;
    private Switch switchDailySummary;
    private Switch switchWeeklyReport;
    private Button btnSavePreferences;
    private Button btnLogout;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getUserPreferencesRepository();
        bindViews(view);
        setupListeners();
        loadPreferences();
    }

    private void bindViews(View view) {
        cbGoalFitness = view.findViewById(R.id.cbGoalFitness);
        cbGoalStudy = view.findViewById(R.id.cbGoalStudy);
        cbGoalProductivity = view.findViewById(R.id.cbGoalProductivity);
        cbGoalFinance = view.findViewById(R.id.cbGoalFinance);
        rgNotificationIntensity = view.findViewById(R.id.rgNotificationIntensity);
        rbNormal = view.findViewById(R.id.rbNormal);
        rbBrutal = view.findViewById(R.id.rbBrutal);
        switchDailySummary = view.findViewById(R.id.switchDailySummary);
        switchWeeklyReport = view.findViewById(R.id.switchWeeklyReport);
        btnSavePreferences = view.findViewById(R.id.btnSavePreferences);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnSavePreferences.setOnClickListener(v -> savePreferences());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void loadPreferences() {
        repository.getUserPreferences(new com.sai.decisiongraveyard.repository.UserPreferencesRepository.PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences preferences) {
                if (preferences.getSelectedGoals() != null) {
                    for (String goal : preferences.getSelectedGoals()) {
                        switch (goal) {
                            case "fitness":
                                cbGoalFitness.setChecked(true);
                                break;
                            case "study":
                                cbGoalStudy.setChecked(true);
                                break;
                            case "productivity":
                                cbGoalProductivity.setChecked(true);
                                break;
                            case "finance":
                                cbGoalFinance.setChecked(true);
                                break;
                        }
                    }
                }

                if ("brutal".equals(preferences.getNotificationIntensity())) {
                    rbBrutal.setChecked(true);
                } else {
                    rbNormal.setChecked(true);
                }

                switchDailySummary.setChecked(preferences.isDailySummaryEnabled());
                switchWeeklyReport.setChecked(preferences.isWeeklyReportEnabled());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePreferences() {
        List<String> selectedGoals = new ArrayList<>();
        if (cbGoalFitness.isChecked()) selectedGoals.add("fitness");
        if (cbGoalStudy.isChecked()) selectedGoals.add("study");
        if (cbGoalProductivity.isChecked()) selectedGoals.add("productivity");
        if (cbGoalFinance.isChecked()) selectedGoals.add("finance");

        String intensity = rbBrutal.isChecked() ? "brutal" : "normal";
        boolean dailySummary = switchDailySummary.isChecked();
        boolean weeklyReport = switchWeeklyReport.isChecked();

        UserPreferences preferences = new UserPreferences();
        preferences.setSelectedGoals(selectedGoals);
        preferences.setNotificationIntensity(intensity);
        preferences.setDailySummaryEnabled(dailySummary);
        preferences.setWeeklyReportEnabled(weeklyReport);

        com.sai.decisiongraveyard.repository.UserPreferencesRepository prefRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getUserPreferencesRepository();
        prefRepo.updateUserPreferences(preferences, new com.sai.decisiongraveyard.repository.UserPreferencesRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Preferences saved", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
