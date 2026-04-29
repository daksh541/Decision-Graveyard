package com.sai.decisiongraveyard.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.ui.achievements.AchievementManager;
import com.sai.decisiongraveyard.viewmodel.DashboardViewModel;

import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private AchievementManager achievementManager;
    private TextView tvLevelName;
    private TextView tvLevelNumber;
    private TextView tvXPProgress;
    private ProgressBar progressXP;
    private TextView tvDisciplineStreak;
    private TextView tvMissedStreak;
    private TextView tvDecisionScore;
    private TextView tvCompletionRate;
    private TextView tvTodayDecisions;
    private TextView tvTodayActivities;
    private TextView tvBehavioralInsight;
    private TextView tvWeeklySummary;
    private TextView tvDay1, tvDay2, tvDay3, tvDay4, tvDay5, tvDay6, tvDay7;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        achievementManager = new AchievementManager(requireContext());
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this,
                androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(DashboardViewModel.class);
        bindViews(view);
        observeViewModel();

        RepositoryProvider provider = RepositoryProvider.getInstance(requireContext());
        provider.getDecisionRepository().getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> viewModel.loadDashboardData());
        provider.getActivityRepository().getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> viewModel.loadDashboardData());
        provider.getUserProfileRepository().getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> viewModel.loadDashboardData());
        provider.getDailyCheckInRepository().getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> viewModel.loadDashboardData());
    }

    private void bindViews(View view) {
        tvLevelName = view.findViewById(R.id.tvLevelName);
        tvLevelNumber = view.findViewById(R.id.tvLevelNumber);
        tvXPProgress = view.findViewById(R.id.tvXPProgress);
        progressXP = view.findViewById(R.id.progressXP);
        tvDisciplineStreak = view.findViewById(R.id.tvDisciplineStreak);
        tvMissedStreak = view.findViewById(R.id.tvMissedStreak);
        tvDecisionScore = view.findViewById(R.id.tvDecisionScore);
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate);
        tvTodayDecisions = view.findViewById(R.id.tvTodayDecisions);
        tvTodayActivities = view.findViewById(R.id.tvTodayActivities);
        tvBehavioralInsight = view.findViewById(R.id.tvBehavioralInsight);
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary);
        tvDay1 = view.findViewById(R.id.tvDay1);
        tvDay2 = view.findViewById(R.id.tvDay2);
        tvDay3 = view.findViewById(R.id.tvDay3);
        tvDay4 = view.findViewById(R.id.tvDay4);
        tvDay5 = view.findViewById(R.id.tvDay5);
        tvDay6 = view.findViewById(R.id.tvDay6);
        tvDay7 = view.findViewById(R.id.tvDay7);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);
        viewModel.getDecisionScore().observe(getViewLifecycleOwner(), score -> {
            tvDecisionScore.setText(score + "%");
        });
        viewModel.getActivityCompletionRate().observe(getViewLifecycleOwner(), rate -> {
            tvCompletionRate.setText(String.format("%.0f%%", rate));
        });
        viewModel.getTodayActivitiesCount().observe(getViewLifecycleOwner(), count -> {
            tvTodayActivities.setText(count);
        });
        viewModel.getTodayDecisionsCount().observe(getViewLifecycleOwner(), count -> {
            tvTodayDecisions.setText(String.valueOf(count));
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void updateProfileUI(UserProfile profile) {
        if (profile == null) return;

        tvLevelName.setText(profile.getLevelName());
        tvLevelNumber.setText("Level " + profile.getCurrentLevel());
        tvXPProgress.setText(profile.getCurrentXP() + " / " + profile.getXpToNextLevel() + " XP");
        progressXP.setProgress((int) profile.getProgressPercentage());
        tvDisciplineStreak.setText(String.valueOf(profile.getDailyDisciplineStreak()));
        tvMissedStreak.setText(String.valueOf(profile.getMissedDayStreak()));

        // Check for achievements
        if (achievementManager != null) {
            achievementManager.checkStreak(profile.getDailyDisciplineStreak());
            achievementManager.checkLevelUp(profile.getCurrentLevel());
        }

        updateBehavioralInsight(profile);
        updateWeeklyTrend(profile);
    }

    private void updateBehavioralInsight(UserProfile profile) {
        String insight;
        int streak = profile.getDailyDisciplineStreak();
        double decisionRate = profile.getDecisionQualityRate();
        double activityRate = profile.getActivityCompletionRate();

        if (streak >= 7) {
            insight = "Amazing! You're on a " + streak + "-day discipline streak. Keep it up!";
        } else if (streak >= 3) {
            insight = "Good momentum! " + streak + " days of discipline. Build on this.";
        } else if (decisionRate < 50 && activityRate < 50) {
            insight = "Focus on improving both decisions and activities. Start small.";
        } else if (decisionRate < 50) {
            insight = "Your decision quality needs attention. Take more time before deciding.";
        } else if (activityRate < 50) {
            insight = "Activity completion is low. Start with smaller, achievable tasks.";
        } else {
            insight = "You're making progress. Track consistently to see improvement.";
        }

        tvBehavioralInsight.setText(insight);
    }

    private void updateWeeklyTrend(UserProfile profile) {
        // Simple weekly trend visualization based on streak
        // In a real implementation, this would pull actual daily data from the repository
        int streak = profile.getDailyDisciplineStreak();
        TextView[] dayViews = {tvDay7, tvDay6, tvDay5, tvDay4, tvDay3, tvDay2, tvDay1};
        
        // Fill recent days with success indicators based on streak
        for (int i = 0; i < dayViews.length; i++) {
            if (i < streak && streak > 0) {
                dayViews[i].setText("✓");
                dayViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
            } else {
                dayViews[i].setText("•");
                dayViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_tertiary));
            }
        }

        // Update summary
        if (streak >= 7) {
            tvWeeklySummary.setText("Perfect week!");
            tvWeeklySummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        } else if (streak >= 5) {
            tvWeeklySummary.setText("Great progress");
            tvWeeklySummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        } else if (streak >= 3) {
            tvWeeklySummary.setText("Building momentum");
            tvWeeklySummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            tvWeeklySummary.setText("Start today");
            tvWeeklySummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadDashboardData();
        }
    }
}
