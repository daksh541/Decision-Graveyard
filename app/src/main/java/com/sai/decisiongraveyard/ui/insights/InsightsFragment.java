package com.sai.decisiongraveyard.ui.insights;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.InsightAdapter;
import com.sai.decisiongraveyard.model.AiInsightReport;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.ui.widgets.HabitHeatmapView;
import com.sai.decisiongraveyard.ui.widgets.RiskMeterView;
import com.sai.decisiongraveyard.ui.widgets.TrendSparklineView;
import com.sai.decisiongraveyard.viewmodel.InsightsViewModel;

import java.util.ArrayList;
import java.util.List;

public class InsightsFragment extends Fragment {

    private InsightsViewModel viewModel;
    private InsightAdapter insightAdapter;

    private ProgressBar progressQuality;
    private ProgressBar progressBar;
    private TextView tvQualityScore;
    private TextView tvQualitySummary;
    private TextView tvIdentityLabel;
    private TextView tvRiskValue;
    private TextView tvWeeklyReport;
    private TextView tvBrainPattern;
    private TextView tvQuote;
    private TextView tvGoodPct;
    private TextView tvBadPct;
    private TextView tvNeutralPct;
    private TextView tvStreakCount;
    private TextView tvBadStreakCount;
    private TextView tvConsequencePoints;
    private TextView tvConsequenceMessage;
    private MaterialCardView cardEmptyInsights;
    private MaterialCardView cardBadStreak;
    private MaterialCardView cardConsequenceMeter;
    private MaterialCardView cardStreak;
    private LinearLayout patternRepetitionContainer;
    private LinearLayout actionableInsightContainer;
    private SwitchMaterial switchBrutalMode;
    private TrendSparklineView trendSparklineView;
    private HabitHeatmapView heatmapView;
    private RiskMeterView riskMeterView;
    private MaterialButton btnRetry;

    public InsightsFragment() {
        super(R.layout.fragment_insights);
    }

    public static InsightsFragment newInstance() {
        return new InsightsFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(InsightsViewModel.class);

        DecisionRepository repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();
        repository.getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> {
            if (trigger != null) {
                viewModel.refresh();
            }
        });

        bindViews(view);
        setupRecyclerView(view);

        switchBrutalMode.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setBrutalMode(isChecked));
        btnRetry.setOnClickListener(v -> viewModel.refresh());

        viewModel.getAnalyticsSnapshot().observe(getViewLifecycleOwner(), this::renderInsights);
        viewModel.getStreakCount().observe(getViewLifecycleOwner(), streak -> {
            tvStreakCount.setText(String.valueOf(streak));
            cardStreak.setVisibility(streak > 1 ? View.VISIBLE : View.GONE);
        });
        viewModel.getBadStreakCount().observe(getViewLifecycleOwner(), streak -> {
            tvBadStreakCount.setText(String.valueOf(streak));
            cardBadStreak.setVisibility(streak >= 2 ? View.VISIBLE : View.GONE);
        });
        viewModel.getBrutalMode().observe(getViewLifecycleOwner(), brutal -> {
            if (brutal != null && switchBrutalMode.isChecked() != brutal) {
                switchBrutalMode.setChecked(brutal);
            }
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindViews(View view) {
        progressQuality = view.findViewById(R.id.progressQuality);
        progressBar = view.findViewById(R.id.progressBar);
        tvQualityScore = view.findViewById(R.id.tvQualityScore);
        tvQualitySummary = view.findViewById(R.id.tvQualitySummary);
        tvIdentityLabel = view.findViewById(R.id.tvIdentityLabel);
        tvRiskValue = view.findViewById(R.id.tvRiskValue);
        tvWeeklyReport = view.findViewById(R.id.tvWeeklyReport);
        tvBrainPattern = view.findViewById(R.id.tvBrainPattern);
        tvQuote = view.findViewById(R.id.tvQuote);
        tvGoodPct = view.findViewById(R.id.tvGoodPct);
        tvBadPct = view.findViewById(R.id.tvBadPct);
        tvNeutralPct = view.findViewById(R.id.tvNeutralPct);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        tvBadStreakCount = view.findViewById(R.id.tvBadStreakCount);
        tvConsequencePoints = view.findViewById(R.id.tvConsequencePoints);
        tvConsequenceMessage = view.findViewById(R.id.tvConsequenceMessage);
        cardEmptyInsights = view.findViewById(R.id.cardEmptyInsights);
        cardBadStreak = view.findViewById(R.id.cardBadStreak);
        cardConsequenceMeter = view.findViewById(R.id.cardConsequenceMeter);
        cardStreak = view.findViewById(R.id.cardStreak);
        patternRepetitionContainer = view.findViewById(R.id.patternRepetitionContainer);
        actionableInsightContainer = view.findViewById(R.id.actionableInsightContainer);
        switchBrutalMode = view.findViewById(R.id.switchBrutalMode);
        trendSparklineView = view.findViewById(R.id.trendSparklineView);
        heatmapView = view.findViewById(R.id.heatmapView);
        riskMeterView = view.findViewById(R.id.riskMeterView);
        btnRetry = view.findViewById(R.id.btnRetry);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerInsights = view.findViewById(R.id.recyclerInsights);
        insightAdapter = new InsightAdapter();
        recyclerInsights.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerInsights.setAdapter(insightAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void renderInsights(AnalyticsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        boolean empty = snapshot.isEmpty();
        cardEmptyInsights.setVisibility(empty ? View.VISIBLE : View.GONE);

        AiInsightReport report = snapshot.getAiInsightReport();
        int qualityScore = report.getBehaviorScore() > 0 ? report.getBehaviorScore() : snapshot.getQualityScore();
        animateProgress(progressQuality, qualityScore);
        tvQualityScore.setText(String.valueOf(qualityScore));
        tvGoodPct.setText(snapshot.getGoodPercent() + "%");
        tvBadPct.setText(snapshot.getBadPercent() + "%");
        tvNeutralPct.setText(snapshot.getNeutralPercent() + "%");
        tvConsequencePoints.setText(String.valueOf(snapshot.getConsequencePoints()));
        tvConsequenceMessage.setText(snapshot.getConsequenceMessage().isEmpty()
                ? "Your behavior trend is still forming."
                : snapshot.getConsequenceMessage());
        tvIdentityLabel.setText(resolveZoneLabel(qualityScore));
        tvIdentityLabel.setTextColor(ContextCompat.getColor(requireContext(), resolveIdentityColor(qualityScore)));
        tvQualitySummary.setText(report.getSummary().isEmpty() ? resolveSummary(snapshot) : report.getSummary());
        tvWeeklyReport.setText(report.getWeeklyReport().isEmpty() ? "No weekly report yet." : report.getWeeklyReport());
        tvBrainPattern.setText(report.getBrainPattern().isEmpty() ? "Your dominant pattern will appear once more reviews land." : report.getBrainPattern());
        tvQuote.setText(report.getQuote().isEmpty() ? "Consistency sharpens clarity." : report.getQuote());
        tvRiskValue.setText(String.format("%d%%", report.getRiskScore()));
        riskMeterView.setRiskValue(report.getRiskScore());
        trendSparklineView.setValues(snapshot.getWeeklyQualityTrend());
        heatmapView.setCells(snapshot.getHeatmapCells());

        patternRepetitionContainer.removeAllViews();
        actionableInsightContainer.removeAllViews();

        for (AnalyticsSnapshot.PatternRepetition pattern : snapshot.getPatternRepetitions()) {
            patternRepetitionContainer.addView(buildMessageCard(pattern.getMessage(), R.color.danger_container, R.color.danger));
        }

        for (String insight : report.getRecommendations()) {
            actionableInsightContainer.addView(buildMessageCard(insight, R.color.success_container, R.color.success));
        }
        if (report.getRecommendations().isEmpty()) {
            for (String insight : snapshot.getActionableInsights()) {
                actionableInsightContainer.addView(buildMessageCard(insight, R.color.success_container, R.color.success));
            }
        }

        List<String> feed = new ArrayList<>();
        feed.addAll(report.getInsightCards());
        feed.addAll(snapshot.getActivityInsights());
        if (feed.isEmpty()) {
            feed.addAll(snapshot.getGeneratedInsights());
        }
        insightAdapter.submitList(feed);

        cardConsequenceMeter.setVisibility(snapshot.getTotalEvaluated() > 0 ? View.VISIBLE : View.GONE);
    }

    private View buildMessageCard(String text, int backgroundColor, int accentColor) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), backgroundColor));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), accentColor));
        card.setStrokeWidth(2);
        card.setRadius(22f);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.bottomMargin = 12;
        card.setLayoutParams(layoutParams);

        TextView textView = new TextView(requireContext());
        textView.setPadding(18, 18, 18, 18);
        textView.setText(text);
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        textView.setTextSize(15f);
        textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(textView);
        return card;
    }

    private String resolveZoneLabel(int score) {
        if (score >= 80) {
            return "Discipline zone";
        }
        if (score >= 60) {
            return "Stabilizing zone";
        }
        if (score >= 40) {
            return "Inconsistent zone";
        }
        return "Self-sabotage zone";
    }

    private String resolveSummary(AnalyticsSnapshot snapshot) {
        if (snapshot.getBadPercent() >= 50) {
            return "Your bad decisions are outweighing your good ones right now.";
        }
        if (snapshot.getNeutralPercent() >= 40) {
            return "You are hesitating too often. Your execution is inconsistent.";
        }
        if (snapshot.getGoodPercent() >= 70) {
            return "You improved this week. The pattern is becoming more deliberate.";
        }
        return snapshot.getTotalEvaluated() + " decisions evaluated. The signal is getting clearer.";
    }

    private int resolveIdentityColor(int score) {
        if (score >= 80) {
            return R.color.success;
        }
        if (score >= 60) {
            return R.color.primary;
        }
        if (score >= 40) {
            return R.color.warning;
        }
        return R.color.danger;
    }

    private void animateProgress(ProgressBar progressBar, int target) {
        ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), target);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> progressBar.setProgress((int) animation.getAnimatedValue()));
        animator.start();
    }
}
