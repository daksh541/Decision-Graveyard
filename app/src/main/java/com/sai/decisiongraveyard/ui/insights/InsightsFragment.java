package com.sai.decisiongraveyard.ui.insights;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.adapter.InsightAdapter;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.viewmodel.InsightsViewModel;

import java.util.ArrayList;
import java.util.List;

public class InsightsFragment extends Fragment {

    private InsightsViewModel viewModel;
    private InsightAdapter insightAdapter;

    private ProgressBar progressQuality;
    private TextView tvQualityScore;
    private TextView tvQualitySummary;
    private TextView tvIdentityLabel;
    private SwitchMaterial switchBrutalMode;
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

        progressQuality = view.findViewById(R.id.progressQuality);
        tvQualityScore = view.findViewById(R.id.tvQualityScore);
        tvQualitySummary = view.findViewById(R.id.tvQualitySummary);
        tvIdentityLabel = view.findViewById(R.id.tvIdentityLabel);
        switchBrutalMode = view.findViewById(R.id.switchBrutalMode);
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
        RecyclerView recyclerInsights = view.findViewById(R.id.recyclerInsights);

        insightAdapter = new InsightAdapter();
        recyclerInsights.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerInsights.setAdapter(insightAdapter);

        switchBrutalMode.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setBrutalMode(isChecked));

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
            if (brutal != null) {
                switchBrutalMode.setChecked(brutal);
            }
        });
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

        int qualityScore = snapshot.getQualityScore();
        animateProgress(progressQuality, qualityScore);
        tvQualityScore.setText(String.valueOf(qualityScore));
        tvGoodPct.setText(snapshot.getGoodPercent() + "%");
        tvBadPct.setText(snapshot.getBadPercent() + "%");
        tvNeutralPct.setText(snapshot.getNeutralPercent() + "%");
        tvConsequencePoints.setText(String.valueOf(snapshot.getConsequencePoints()));
        tvConsequenceMessage.setText(snapshot.getConsequenceMessage().isEmpty()
                ? "Your behavior trend is still forming."
                : snapshot.getConsequenceMessage());

        cardConsequenceMeter.setVisibility(snapshot.getTotalEvaluated() > 0 ? View.VISIBLE : View.GONE);
        tvIdentityLabel.setText(resolveZoneLabel(snapshot));
        tvIdentityLabel.setTextColor(ContextCompat.getColor(requireContext(), resolveIdentityColor(snapshot)));
        tvQualitySummary.setText(resolveSummary(snapshot));
        tvConsequencePoints.setTextColor(ContextCompat.getColor(requireContext(), resolveConsequenceColor(snapshot.getConsequencePoints())));

        patternRepetitionContainer.removeAllViews();
        actionableInsightContainer.removeAllViews();

        for (AnalyticsSnapshot.PatternRepetition pattern : snapshot.getPatternRepetitions()) {
            patternRepetitionContainer.addView(buildMessageCard(pattern.getMessage(), R.color.danger_container, R.color.danger));
        }

        for (String insight : snapshot.getActionableInsights()) {
            actionableInsightContainer.addView(buildMessageCard(insight, R.color.success_container, R.color.success));
        }

        List<String> feed = new ArrayList<>();
        feed.addAll(snapshot.getGeneratedInsights());
        if (feed.isEmpty() && !snapshot.getActionableInsights().isEmpty()) {
            feed.addAll(snapshot.getActionableInsights());
        }
        insightAdapter.submitList(feed);
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

    private String resolveZoneLabel(AnalyticsSnapshot snapshot) {
        int score = snapshot.getQualityScore();
        if (score >= 80) {
            return "You are in Discipline Zone";
        }
        if (score >= 60) {
            return "You are in Stabilizing Zone";
        }
        if (score >= 40) {
            return "You are in Inconsistent Zone";
        }
        return "You are in Self-Sabotage Zone";
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

    private int resolveIdentityColor(AnalyticsSnapshot snapshot) {
        int score = snapshot.getQualityScore();
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

    private int resolveConsequenceColor(int points) {
        if (points > 0) {
            return R.color.success;
        }
        if (points < 0) {
            return R.color.danger;
        }
        return R.color.text_primary;
    }

    private void animateProgress(ProgressBar progressBar, int target) {
        ValueAnimator animator = ValueAnimator.ofInt(progressBar.getProgress(), target);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> progressBar.setProgress((int) animation.getAnimatedValue()));
        animator.start();
    }
}
