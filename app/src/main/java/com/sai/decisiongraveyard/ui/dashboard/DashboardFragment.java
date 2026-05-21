package com.sai.decisiongraveyard.ui.dashboard;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.AiInsightReport;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.ui.activities.ActivityListFragment;
import com.sai.decisiongraveyard.ui.add.AddDecisionFragment;
import com.sai.decisiongraveyard.ui.achievements.AchievementManager;
import com.sai.decisiongraveyard.ui.insights.InsightsFragment;
import com.sai.decisiongraveyard.ui.timeline.TimelineFragment;
import com.sai.decisiongraveyard.ui.widgets.DecisionScoreRingView;
import com.sai.decisiongraveyard.ui.widgets.HabitHeatmapView;
import com.sai.decisiongraveyard.ui.widgets.RiskMeterView;
import com.sai.decisiongraveyard.ui.widgets.TrendSparklineView;
import com.sai.decisiongraveyard.util.ViewPressAnimations;
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
    private TextView tvBrainPattern;
    private TextView tvWeeklySignal;
    private TextView tvQuote;
    private TextView tvRiskValue;
    private TextView tvBehaviorSummary;
    private TextView tvPriorityOne;
    private TextView tvPriorityTwo;
    private TextView tvPriorityThree;
    private TrendSparklineView trendSparklineView;
    private HabitHeatmapView heatmapView;
    private RiskMeterView riskMeterView;
    private ProgressBar progressBar;

    private int todayCompleted;
    private int todayMissed;
    private int todayRisky;
    private double activityRate;
    private AiInsightReport latestReport = AiInsightReport.empty();

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
        tvBrainPattern = view.findViewById(R.id.tvBrainPattern);
        tvWeeklySignal = view.findViewById(R.id.tvWeeklySignal);
        tvQuote = view.findViewById(R.id.tvQuote);
        tvRiskValue = view.findViewById(R.id.tvRiskValue);
        tvBehaviorSummary = view.findViewById(R.id.tvBehaviorSummary);
        tvPriorityOne = view.findViewById(R.id.tvPriorityOne);
        tvPriorityTwo = view.findViewById(R.id.tvPriorityTwo);
        tvPriorityThree = view.findViewById(R.id.tvPriorityThree);
        trendSparklineView = view.findViewById(R.id.trendSparklineView);
        heatmapView = view.findViewById(R.id.heatmapView);
        riskMeterView = view.findViewById(R.id.riskMeterView);
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

        animateCard(view.findViewById(R.id.cardHeroScore), 0L);
        animateCard(view.findViewById(R.id.cardTodaySummary), 90L);
        animateCard(view.findViewById(R.id.cardPatternPulse), 160L);
        animateCard(view.findViewById(R.id.cardWeeklyArc), 220L);
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateProfileUI);
        viewModel.getDecisionScore().observe(getViewLifecycleOwner(), score -> {
            if (!latestReport.isAiGenerated() && latestReport.getBehaviorScore() == 0) {
                scoreRing.setProgress(score);
                tvDecisionScore.setText(String.valueOf(score));
            }
        });
        viewModel.getActivityCompletionRate().observe(getViewLifecycleOwner(), rate -> {
            activityRate = rate;
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
        viewModel.getAnalyticsSnapshot().observe(getViewLifecycleOwner(), this::bindAnalyticsSnapshot);
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

    private void bindAnalyticsSnapshot(AnalyticsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        latestReport = snapshot.getAiInsightReport();
        int displayScore = latestReport.getBehaviorScore() > 0 ? latestReport.getBehaviorScore() : snapshot.getQualityScore();
        scoreRing.setProgress(displayScore);
        tvDecisionScore.setText(String.valueOf(displayScore));
        tvScoreTrend.setText(latestReport.getCoachPersona().isEmpty() ? "Behavior score" : latestReport.getCoachPersona());
        tvBehaviorSummary.setText(latestReport.getSummary().isEmpty() ? "Your behavioral signal is still forming." : latestReport.getSummary());
        tvBrainPattern.setText(latestReport.getBrainPattern().isEmpty() ? "Track more decisions to reveal your dominant pattern." : latestReport.getBrainPattern());
        tvWeeklySignal.setText(latestReport.getWeeklyReport().isEmpty() ? "No weekly report yet." : latestReport.getWeeklyReport());
        tvQuote.setText(latestReport.getQuote().isEmpty() ? "Small disciplined choices compound." : latestReport.getQuote());
        tvRiskValue.setText(String.format(Locale.getDefault(), "%d%%", latestReport.getRiskScore()));
        riskMeterView.setRiskValue(latestReport.getRiskScore());
        trendSparklineView.setValues(snapshot.getWeeklyQualityTrend());
        heatmapView.setCells(snapshot.getHeatmapCells());
        bindPriorities(latestReport);
        updateCoachMessage();
    }

    private void bindPriorities(AiInsightReport report) {
        String first = report.getWeeklyPriorities().size() > 0 ? report.getWeeklyPriorities().get(0) : "Protect your next clean routine.";
        String second = report.getWeeklyPriorities().size() > 1 ? report.getWeeklyPriorities().get(1) : "Reduce friction before weak hours.";
        String third = report.getWeeklyPriorities().size() > 2 ? report.getWeeklyPriorities().get(2) : "Close the loop on overdue reviews.";
        tvPriorityOne.setText(first);
        tvPriorityTwo.setText(second);
        tvPriorityThree.setText(third);
    }

    private void updateProfileUI(UserProfile profile) {
        if (profile == null) {
            return;
        }

        tvLevelBadge.setText(String.format(Locale.getDefault(), "LVL %d", profile.getCurrentLevel()));
        tvXPProgress.setText(profile.getCurrentXP() + " / " + profile.getXpToNextLevel());
        animateProgress(progressXP, (int) profile.getProgressPercentage());
        tvDisciplineStreak.setText(String.format(Locale.getDefault(), "%d day streak", profile.getDailyDisciplineStreak()));
        tvStreakWarning.setText(resolveStreakWarning(profile));

        if (achievementManager != null) {
            achievementManager.checkStreak(profile.getDailyDisciplineStreak());
            achievementManager.checkLevelUp(profile.getCurrentLevel());
        }

        updateCoachMessage();
    }

    private void updateCoachMessage() {
        if (latestReport != null && !latestReport.getRecoverySuggestion().isEmpty()) {
            tvCoachMessage.setText(latestReport.getRecoverySuggestion());
            return;
        }

        if (todayMissed > 0 && todayCompleted == 0) {
            tvCoachMessage.setText("You already slipped today. Recover with one clean action now.");
            return;
        }
        if (todayRisky > 0) {
            tvCoachMessage.setText("Risk is clustering today. Slow down before your next decision.");
            return;
        }
        if (todayCompleted >= 3 && activityRate >= 70) {
            tvCoachMessage.setText("Momentum is clean today. Keep pressing the advantage.");
            return;
        }
        tvCoachMessage.setText("One deliberate action can still improve the whole day.");
    }

    private String resolveGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Morning signal";
        }
        if (hour < 18) {
            return "Daily pulse";
        }
        return "Night watch";
    }

    private String resolveStreakWarning(UserProfile profile) {
        if (profile.getDailyDisciplineStreak() >= 7) {
            return "Momentum locked";
        }
        if (profile.getMissedDayStreak() > 0) {
            return "Drift detected";
        }
        return "Signal live";
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

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    private void animateCard(@Nullable View view, long delayMs) {
        if (view == null) {
            return;
        }
        view.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.card_enter));
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(380L)
                .start();
    }

    private void animateProgress(ProgressBar progressBar, int target) {
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), target)
                .setDuration(650L)
                .start();
    }
}
