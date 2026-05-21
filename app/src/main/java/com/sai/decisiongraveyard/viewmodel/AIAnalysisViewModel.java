package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.AiInsightReport;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIAnalysisViewModel extends AndroidViewModel {

    private final MutableLiveData<String> analysisResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AIAnalysisViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getAnalysisResult() {
        return analysisResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void generateAnalysis(DecisionRepository repository) {
        isLoading.setValue(true);
        error.setValue(null);

        executorService.execute(() -> {
            try {
                List<DecisionRecord> records = repository.getAllDecisionRecords();
                
                if (records.isEmpty()) {
                    mainHandler.post(() -> {
                        error.setValue("No decisions to analyze");
                        isLoading.setValue(false);
                    });
                    return;
                }

                AnalyticsSnapshot snapshot = repository.getAnalyticsSnapshot(false);
                String analysis = buildNarrative(snapshot);

                mainHandler.post(() -> {
                    analysisResult.setValue(analysis);
                    isLoading.setValue(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    error.setValue("Failed to generate analysis: " + e.getMessage());
                    isLoading.setValue(false);
                });
            }
        });
    }

    private String buildNarrative(AnalyticsSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        AiInsightReport report = snapshot.getAiInsightReport();
        sb.append(report.getCoachPersona().isEmpty() ? "Signal Coach" : report.getCoachPersona()).append("\n\n");
        sb.append(report.getSummary()).append("\n\n");
        sb.append("Behavior score: ").append(report.getBehaviorScore()).append("/100\n");
        sb.append("Discipline score: ").append(report.getDisciplineScore()).append("/100\n");
        sb.append("Risk pressure: ").append(report.getRiskScore()).append("/100\n\n");

        if (!report.getBrainPattern().isEmpty()) {
            sb.append("Brain pattern\n").append(report.getBrainPattern()).append("\n\n");
        }
        if (!report.getWeeklyReport().isEmpty()) {
            sb.append("Weekly report\n").append(report.getWeeklyReport()).append("\n\n");
        }
        if (!report.getEmotionalPattern().isEmpty()) {
            sb.append("Emotional pattern\n").append(report.getEmotionalPattern()).append("\n\n");
        }
        if (!report.getActivityCorrelation().isEmpty()) {
            sb.append("Activity correlation\n").append(report.getActivityCorrelation()).append("\n\n");
        }
        if (!report.getRecoverySuggestion().isEmpty()) {
            sb.append("Recovery suggestion\n").append(report.getRecoverySuggestion()).append("\n\n");
        }

        if (!report.getInsightCards().isEmpty()) {
            sb.append("Key insights\n");
            for (String insight : report.getInsightCards()) {
                sb.append("• ").append(insight).append('\n');
            }
            sb.append('\n');
        }
        if (!report.getRecommendations().isEmpty()) {
            sb.append("Recommended next moves\n");
            for (String insight : report.getRecommendations()) {
                sb.append("• ").append(insight).append('\n');
            }
            sb.append('\n');
        }
        if (!report.getQuote().isEmpty()) {
            sb.append(report.getQuote());
        }
        return sb.toString();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
