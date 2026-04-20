package com.sai.decisiongraveyard.ui.insights;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.InsightAdapter;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot.PatternRepetition;
import com.sai.decisiongraveyard.model.CategoryInsight;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.ui.charts.CategoryBarChartView;
import com.sai.decisiongraveyard.util.DateUtils;
import com.sai.decisiongraveyard.viewmodel.InsightsViewModel;

import java.util.ArrayList;
import java.util.List;

public class InsightsFragment extends Fragment {

    private InsightsViewModel viewModel;
    private InsightAdapter insightAdapter;

    private ProgressBar progressQuality;
    private TextView tvQualityScore;
    private TextView tvQualitySummary;
    private TextView tvEmojiFeedback;
    private TextView tvIdentityLabel;
    private SwitchMaterial switchBrutalMode;
    private TextView tvGoodPct;
    private TextView tvBadPct;
    private TextView tvNeutralPct;
    private TextView tvStreakCount;
    private TextView tvBadStreakCount;
    private MaterialCardView cardEmptyInsights;
    private MaterialCardView cardBadStreak;
    private MaterialCardView cardConsequenceMeter;
    private TextView tvConsequencePoints;
    private TextView tvConsequenceMessage;
    private MaterialCardView cardPatternRepetitions;
    private MaterialCardView cardActionableInsights;
    private LinearLayout categoryInsightContainer;
    private LinearLayout patternRepetitionContainer;
    private LinearLayout actionableInsightContainer;
    private MaterialCardView cardCategoryInsight;
    private MaterialCardView cardGeneratedInsights;
    private MaterialCardView cardStreak;
    private CategoryBarChartView categoryBarChart;

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

        // Observe repository data changes for immediate refresh
        DecisionRepository repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();
        repository.getDataChangedTrigger().observe(getViewLifecycleOwner(), trigger -> {
            if (trigger != null) {
                viewModel.refresh();
            }
        });

        progressQuality = view.findViewById(R.id.progressQuality);
        tvQualityScore = view.findViewById(R.id.tvQualityScore);
        tvQualitySummary = view.findViewById(R.id.tvQualitySummary);
        tvEmojiFeedback = view.findViewById(R.id.tvEmojiFeedback);
        tvIdentityLabel = view.findViewById(R.id.tvIdentityLabel);
        switchBrutalMode = view.findViewById(R.id.switchBrutalMode);
        tvGoodPct = view.findViewById(R.id.tvGoodPct);
        tvBadPct = view.findViewById(R.id.tvBadPct);
        tvNeutralPct = view.findViewById(R.id.tvNeutralPct);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        tvBadStreakCount = view.findViewById(R.id.tvBadStreakCount);
        cardEmptyInsights = view.findViewById(R.id.cardEmptyInsights);
        cardBadStreak = view.findViewById(R.id.cardBadStreak);
        cardConsequenceMeter = view.findViewById(R.id.cardConsequenceMeter);
        tvConsequencePoints = view.findViewById(R.id.tvConsequencePoints);
        tvConsequenceMessage = view.findViewById(R.id.tvConsequenceMessage);
        cardPatternRepetitions = view.findViewById(R.id.cardPatternRepetitions);
        cardActionableInsights = view.findViewById(R.id.cardActionableInsights);
        categoryInsightContainer = view.findViewById(R.id.categoryInsightContainer);
        patternRepetitionContainer = view.findViewById(R.id.patternRepetitionContainer);
        actionableInsightContainer = view.findViewById(R.id.actionableInsightContainer);
        cardCategoryInsight = view.findViewById(R.id.cardCategoryInsight);
        cardGeneratedInsights = view.findViewById(R.id.cardGeneratedInsights);
        cardStreak = view.findViewById(R.id.cardStreak);
        categoryBarChart = view.findViewById(R.id.categoryBarChart);
        RecyclerView recyclerInsights = view.findViewById(R.id.recyclerInsights);

        insightAdapter = new InsightAdapter();
        recyclerInsights.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerInsights.setAdapter(insightAdapter);

        // Set up brutal mode toggle
        switchBrutalMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setBrutalMode(isChecked);
        });

        viewModel.getAnalyticsSnapshot().observe(getViewLifecycleOwner(), this::renderInsights);
        viewModel.getStreakCount().observe(getViewLifecycleOwner(), this::updateStreak);
        viewModel.getBadStreakCount().observe(getViewLifecycleOwner(), this::updateBadStreak);
        viewModel.getBrutalMode().observe(getViewLifecycleOwner(), isBrutal -> {
            if (isBrutal != null) {
                switchBrutalMode.setChecked(isBrutal);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    private void renderInsights(AnalyticsSnapshot snapshot) {
        if (snapshot == null) return;

        boolean showEmpty = snapshot.isEmpty();
        cardEmptyInsights.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        cardCategoryInsight.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        cardGeneratedInsights.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        cardStreak.setVisibility(showEmpty ? View.GONE : View.VISIBLE);

        if (showEmpty) {
            insightAdapter.submitList(java.util.Collections.emptyList());
            categoryInsightContainer.removeAllViews();
            patternRepetitionContainer.removeAllViews();
            actionableInsightContainer.removeAllViews();
            tvEmojiFeedback.setText("🤔");
            tvIdentityLabel.setText("Analyzing...");
            cardBadStreak.setVisibility(View.GONE);
            cardPatternRepetitions.setVisibility(View.GONE);
            cardActionableInsights.setVisibility(View.GONE);
            return;
        }

        // Quality Score with animation
        int qualityScore = snapshot.getQualityScore();
        animateProgress(progressQuality, qualityScore);
        tvQualityScore.setText(String.valueOf(qualityScore));
        tvQualitySummary.setText(
                snapshot.getTotalDecisions() + " logged • "
                        + snapshot.getTotalEvaluated() + " evaluated • "
                        + snapshot.getReadyForReviewCount() + " ready"
        );

        // Identity Label
        tvIdentityLabel.setText(snapshot.getIdentityLabel());
        int identityColor = getIdentityColor(snapshot.getIdentityLabel());
        tvIdentityLabel.setTextColor(ContextCompat.getColor(requireContext(), identityColor));

        // Consequence Meter
        cardConsequenceMeter.setVisibility(snapshot.getTotalEvaluated() > 0 ? View.VISIBLE : View.GONE);
        tvConsequencePoints.setText(String.valueOf(snapshot.getConsequencePoints()));
        tvConsequenceMessage.setText(snapshot.getConsequenceMessage());
        int consequenceColor = getConsequenceColor(snapshot.getConsequencePoints());
        tvConsequencePoints.setTextColor(ContextCompat.getColor(requireContext(), consequenceColor));

        // Emoji feedback based on score
        tvEmojiFeedback.setText(getEmojiForScore(qualityScore));

        // Stats
        tvGoodPct.setText(snapshot.getGoodPercent() + "%");
        tvBadPct.setText(snapshot.getBadPercent() + "%");
        tvNeutralPct.setText(snapshot.getNeutralPercent() + "%");

        // Category insights with progress bars
        categoryInsightContainer.removeAllViews();
        List<CategoryBarChartView.BarData> barDataList = new ArrayList<>();
        for (CategoryInsight categoryInsight : snapshot.getCategoryInsights()) {
            if (categoryInsight.getEvaluatedCount() == 0) continue;
            addCategoryInsightView(categoryInsight);
            
            // Add data for bar chart (show success rate instead of regret rate)
            int successRate = 100 - categoryInsight.getRegretRate();
            int color = successRate >= 70 ? R.color.success :
                       successRate >= 50 ? R.color.warning : R.color.danger;
            barDataList.add(new CategoryBarChartView.BarData(
                    DateUtils.getCategoryDisplayName(categoryInsight.getCategory()),
                    successRate,
                    ContextCompat.getColor(requireContext(), color)
            ));
        }
        
        // Update bar chart
        if (!barDataList.isEmpty()) {
            categoryBarChart.setBarData(barDataList);
            categoryBarChart.setVisibility(View.VISIBLE);
        } else {
            categoryBarChart.setVisibility(View.GONE);
        }

        // Pattern Repetitions
        patternRepetitionContainer.removeAllViews();
        if (!snapshot.getPatternRepetitions().isEmpty()) {
            cardPatternRepetitions.setVisibility(View.VISIBLE);
            for (PatternRepetition pattern : snapshot.getPatternRepetitions()) {
                addPatternRepetitionView(pattern);
            }
        } else {
            cardPatternRepetitions.setVisibility(View.GONE);
        }

        // Actionable Insights
        actionableInsightContainer.removeAllViews();
        if (!snapshot.getActionableInsights().isEmpty()) {
            cardActionableInsights.setVisibility(View.VISIBLE);
            for (String insight : snapshot.getActionableInsights()) {
                addActionableInsightView(insight);
            }
        } else {
            cardActionableInsights.setVisibility(View.GONE);
        }

        insightAdapter.submitList(snapshot.getGeneratedInsights());
    }

    private void updateStreak(int streak) {
        tvStreakCount.setText(String.valueOf(streak));
        cardStreak.setVisibility(streak > 1 ? View.VISIBLE : View.GONE);
    }

    private void updateBadStreak(int badStreak) {
        tvBadStreakCount.setText(String.valueOf(badStreak));
        cardBadStreak.setVisibility(badStreak >= 3 ? View.VISIBLE : View.GONE);
    }

    private int getIdentityColor(String label) {
        if (label.contains("Disciplined")) return R.color.success;
        if (label.contains("Controlled")) return R.color.success;
        if (label.contains("Aware")) return R.color.warning;
        return R.color.danger;
    }

    private int getConsequenceColor(int points) {
        if (points >= 20) return R.color.success;
        if (points >= 0) return R.color.text_primary;
        if (points >= -20) return R.color.warning;
        return R.color.danger;
    }

    private void addPatternRepetitionView(PatternRepetition pattern) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 0, 0, 12);
        container.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_surface_card));

        TextView tvPattern = new TextView(requireContext());
        tvPattern.setText("You've made this mistake " + pattern.getCount() + " times in " +
                DateUtils.getCategoryDisplayName(pattern.getCategory()).toLowerCase() + " (" +
                pattern.getTimePattern() + ")");
        tvPattern.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
        tvPattern.setTextSize(14f);
        tvPattern.setPadding(16, 12, 16, 12);

        container.addView(tvPattern);
        patternRepetitionContainer.addView(container);
    }

    private void addActionableInsightView(String insight) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 0, 0, 12);
        container.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_surface_card));

        TextView tvInsight = new TextView(requireContext());
        tvInsight.setText("✓ " + insight);
        tvInsight.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        tvInsight.setTextSize(14f);
        tvInsight.setPadding(16, 12, 16, 12);

        container.addView(tvInsight);
        actionableInsightContainer.addView(container);
    }

    private void animateProgress(ProgressBar progressBar, int target) {
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            progressBar.setProgress((int) animation.getAnimatedValue());
        });
        animator.start();
    }

    private String getEmojiForScore(int score) {
        if (score >= 70) return "😎";
        if (score >= 50) return "🙂";
        if (score >= 30) return "😐";
        return "😬";
    }

    private void addCategoryInsightView(CategoryInsight insight) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 0, 0, 16);

        // Header with category and regret rate
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvCategory = new TextView(requireContext());
        tvCategory.setText(DateUtils.getCategoryDisplayName(insight.getCategory()));
        tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tvCategory.setTextSize(14f);
        tvCategory.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvRate = new TextView(requireContext());
        tvRate.setText(insight.getRegretRate() + "% regret");
        tvRate.setTextColor(ContextCompat.getColor(requireContext(), 
                insight.getRegretRate() > 50 ? R.color.danger : R.color.text_secondary));
        tvRate.setTextSize(12f);

        header.addView(tvCategory);
        header.addView(tvRate);

        // Progress bar
        LinearProgressIndicator progress = new LinearProgressIndicator(requireContext());
        progress.setProgress(insight.getRegretRate());
        progress.setTrackColor(ContextCompat.getColor(requireContext(), R.color.divider));
        int progressColor = insight.getRegretRate() > 50 ? R.color.danger : 
                           (insight.getRegretRate() > 30 ? R.color.warning : R.color.success);
        progress.setIndicatorColor(ContextCompat.getColor(requireContext(), progressColor));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8);
        progressParams.topMargin = 8;
        progress.setLayoutParams(progressParams);

        container.addView(header);
        container.addView(progress);

        categoryInsightContainer.addView(container);
    }
}
