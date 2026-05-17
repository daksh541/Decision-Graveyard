package com.sai.decisiongraveyard.ui.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sai.decisiongraveyard.ui.widgets.DecisionScoreRingView;
import com.sai.decisiongraveyard.util.ViewPressAnimations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.ui.activities.ActivityListFragment;
import com.sai.decisiongraveyard.ui.add.AddDecisionFragment;
import com.sai.decisiongraveyard.ui.achievements.AchievementManager;
import com.sai.decisiongraveyard.ui.insights.InsightsFragment;
import com.sai.decisiongraveyard.ui.timeline.TimelineFragment;
import com.sai.decisiongraveyard.viewmodel.DashboardViewModel;

import java.util.Calendar;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private AchievementManager achievementManager;
    private TextView tvGreeting;
    private TextView tvProfileName;
    private TextView tvLevelBadge;
    private TextView tvXPProgress;
    private ProgressBar progressXP;
    private DecisionScoreRingView scoreRing;
    private TextView tvDecisionScore;
    private TextView tvScoreTrend;
    private TextView tvTodayCompleted;
    private TextView tvTodayMissed;
    private TextView tvTodayRisky;
    private TextView tvDisciplineStreak;
    private TextView tvStreakWarning;
    private TextView tvCoachMessage;
    private ProgressBar progressBar;

    private int todayCompleted;
    private int todayMissed;
    private int todayRisky;
    private double activityRate;

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
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvLevelBadge = view.findViewById(R.id.tvLevelBadge);
        tvXPProgress = view.findViewById(R.id.tvXPProgress);
        progressXP = view.findViewById(R.id.progressXP);
        scoreRing = view.findViewById(R.id.scoreRing);
        tvDecisionScore = view.findViewById(R.id.tvDecisionScore);
        tvScoreTrend = view.findViewById(R.id.tvScoreTrend);
        tvTodayCompleted = view.findViewById(R.id.tvTodayCompleted);
        tvTodayMissed = view.findViewById(R.id.tvTodayMissed);
        tvTodayRisky = view.findViewById(R.id.tvTodayRisky);
        tvDisciplineStreak = view.findViewById(R.id.tvDisciplineStreak);
        tvStreakWarning = view.findViewById(R.id.tvStreakWarning);
        tvCoachMessage = view.findViewById(R.id.tvCoachMessage);
        progressBar = view.findViewById(R.id.progressBar);

        tvGreeting.setText(resolveGreeting());
        tvProfileName.setText(resolveProfileName());
        View btnQuickDecision = view.findViewById(R.id.btnQuickDecision);
        View btnQuickActivities = view.findViewById(R.id.btnQuickActivities);
        View btnQuickTimeline = view.findViewById(R.id.btnQuickTimeline);
        View btnQuickInsights = view.findViewById(R.id.btnQuickInsights);

        btnQuickDecision.setOnClickListener(v -> openFragment(new AddDecisionFragment()));
        btnQuickActivities.setOnClickListener(v -> openFragment(new ActivityListFragment()));
        btnQuickTimeline.setOnClickListener(v -> openFragment(new TimelineFragment()));
        btnQuickInsights.setOnClickListener(v -> openFragment(new InsightsFragment()));

        ViewPressAnimations.attach(btnQuickDecision);
        ViewPressAnimations.attach(btnQuickActivities);
        ViewPressAnimations.attach(btnQuickTimeline);
        ViewPressAnimations.attach(btnQuickInsights);

        View cardHero = view.findViewById(R.id.cardHeroScore);
        View cardSummary = view.findViewById(R.id.cardTodaySummary);
        if (cardHero != null) {
            cardHero.startAnimation(android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.card_enter));
        }
        if (cardSummary != null) {
            android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.card_enter);
            animation.setStartOffset(80);
            cardSummary.startAnimation(animation);
        }
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);
        viewModel.getDecisionScore().observe(getViewLifecycleOwner(), score -> {
            tvDecisionScore.setText(String.valueOf(score));
            scoreRing.setProgress(score);
            updateCoachMessage();
        });
        viewModel.getActivityCompletionRate().observe(getViewLifecycleOwner(), rate -> {
            activityRate = rate;
            tvTodayRisky.setText(String.format(Locale.getDefault(), "%.0f%%", rate));
            updateCoachMessage();
        });
        viewModel.getTodayCompletedActivities().observe(getViewLifecycleOwner(), count -> {
            todayCompleted = count;
            tvTodayCompleted.setText(String.valueOf(count));
            updateCoachMessage();
        });
        viewModel.getTodayMissedActivities().observe(getViewLifecycleOwner(), count -> {
            todayMissed = count;
            tvTodayMissed.setText(String.valueOf(count));
            updateCoachMessage();
        });
        viewModel.getTodayRiskyDecisions().observe(getViewLifecycleOwner(), count -> {
            todayRisky = count;
            tvTodayRisky.setText(String.valueOf(count));
            updateCoachMessage();
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void updateProfileUI(UserProfile profile) {
        if (profile == null) {
            return;
        }

        tvLevelBadge.setText(String.format(Locale.getDefault(), "LVL %d", profile.getCurrentLevel()));
        tvXPProgress.setText(profile.getCurrentXP() + " / " + profile.getXpToNextLevel());
        progressXP.setProgress((int) profile.getProgressPercentage());
        tvDisciplineStreak.setText(String.format(Locale.getDefault(), "%d Day Streak", profile.getDailyDisciplineStreak()));

        if (achievementManager != null) {
            achievementManager.checkStreak(profile.getDailyDisciplineStreak());
            achievementManager.checkLevelUp(profile.getCurrentLevel());
        }

        updateCoachMessage();
    }

    private String resolveGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Welcome back";
        }
        if (hour < 18) {
            return "Back at it";
        }
        return "Keep your edge";
    }

    private String resolveStreakWarning(UserProfile profile) {
        if (profile.getDailyDisciplineStreak() >= 7) {
            return "Locked in. Protect the streak.";
        }
        if (profile.getMissedDayStreak() > 0) {
            return "You are close to breaking momentum.";
        }
        return "Momentum is fragile. Show up today.";
    }

    private void updateCoachMessage() {
        String message;

        if (todayMissed > 0 && todayCompleted == 0) {
            message = "You already slipped today. Recover with one clean action now.";
        } else if (todayRisky > 0) {
            message = "Your risky choices are clustering. Slow down before the next one.";
        } else if (todayCompleted >= 3 && activityRate >= 70) {
            message = "Today feels disciplined. Keep the pressure on.";
        } else if (TextUtils.isEmpty(tvDecisionScore.getText()) || "0".contentEquals(tvDecisionScore.getText())) {
            message = "No pattern yet. Start tracking honestly and the signal will show up.";
        } else {
            message = "You are one deliberate action away from control.";
        }

        tvCoachMessage.setText(message);
        tvStreakWarning.setText("LIVE SIGNAL");
        tvScoreTrend.setText(buildTrendMessage());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadDashboardData();
        }
    }

    private String resolveProfileName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return "Operator";
        }
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        String email = user.getEmail();
        if (TextUtils.isEmpty(email)) {
            return "Operator";
        }
        String local = email.split("@")[0].replace('.', ' ').replace('_', ' ').trim();
        if (local.isEmpty()) {
            return "Operator";
        }
        String[] parts = local.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        return builder.toString().trim();
    }

    private String buildTrendMessage() {
        int delta = todayCompleted - todayRisky - todayMissed;
        if (delta > 0) {
            return String.format(Locale.getDefault(), "+%d today", delta);
        }
        if (todayRisky > 0 || todayMissed > 0) {
            return "Under pressure";
        }
        return "Signal forming";
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
