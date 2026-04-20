package com.sai.decisiongraveyard.ui.ai;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.viewmodel.AIAnalysisViewModel;

public class AIAnalysisFragment extends Fragment {

    private AIAnalysisViewModel viewModel;
    private DecisionRepository repository;

    private TextView tvAnalysisTitle;
    private TextView tvAnalysisContent;
    private TextView tvEmptyState;
    private Button btnGenerateAnalysis;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private MaterialCardView cardAnalysis;

    public AIAnalysisFragment() {
        super(R.layout.fragment_ai_analysis);
    }

    public static AIAnalysisFragment newInstance() {
        return new AIAnalysisFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(requireContext()).getDecisionRepository();
        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(AIAnalysisViewModel.class);

        tvAnalysisTitle = view.findViewById(R.id.tvAnalysisTitle);
        tvAnalysisContent = view.findViewById(R.id.tvAnalysisContent);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnGenerateAnalysis = view.findViewById(R.id.btnGenerateAnalysis);
        progressBar = view.findViewById(R.id.progressBar);
        scrollView = view.findViewById(R.id.scrollView);
        cardAnalysis = view.findViewById(R.id.cardAnalysis);

        btnGenerateAnalysis.setOnClickListener(v -> generateAIAnalysis());

        viewModel.getAnalysisResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                showEmptyState();
                return;
            }
            showAnalysis(result);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnGenerateAnalysis.setEnabled(!isLoading);
            if (isLoading) {
                cardAnalysis.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        checkIfHasData();
    }

    private void checkIfHasData() {
        new Thread(() -> {
            try {
                java.util.List<com.sai.decisiongraveyard.model.DecisionRecord> records = repository.getAllDecisionRecords();
                boolean hasData = !records.isEmpty();
                requireActivity().runOnUiThread(() -> {
                    btnGenerateAnalysis.setEnabled(hasData);
                    if (!hasData) {
                        tvEmptyState.setText("Add some decisions first to get AI-powered insights about your decision patterns.");
                        showEmptyState();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    btnGenerateAnalysis.setEnabled(false);
                    tvEmptyState.setText("Unable to load decisions. Please try again.");
                    showEmptyState();
                });
            }
        }).start();
    }

    private void generateAIAnalysis() {
        viewModel.generateAnalysis(repository);
    }

    private void showAnalysis(String analysis) {
        cardAnalysis.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        tvAnalysisContent.setText(analysis);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
    }

    private void showEmptyState() {
        cardAnalysis.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }
}
