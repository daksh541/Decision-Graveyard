package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.service.LLMAnalysisService;

import java.util.List;
import java.util.Locale;
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

                // Prepare data for LLM
                String decisionData = prepareDecisionData(records);
                
                // Call LLM service
                LLMAnalysisService llmService = new LLMAnalysisService();
                String analysis = llmService.analyzeDecisions(decisionData);

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

    private String prepareDecisionData(List<DecisionRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze these decision records and provide personalized insights:\n\n");
        
        int goodCount = 0;
        int badCount = 0;
        int neutralCount = 0;
        
        for (DecisionRecord record : records) {
            if (record.isEvaluated()) {
                String outcome = record.getDecision().getOutcome();
                if ("good".equals(outcome)) {
                    goodCount++;
                } else if ("bad".equals(outcome)) {
                    badCount++;
                } else {
                    neutralCount++;
                }
                
                sb.append(String.format(Locale.getDefault(), "- Decision: %s\n", record.getDecision().getTitle()));
                sb.append(String.format(Locale.getDefault(), "  Category: %s\n", record.getDecision().getCategory()));
                sb.append(String.format(Locale.getDefault(), "  Outcome: %s\n", outcome));
                if (record.getDecision().getReflectionNotes() != null && !record.getDecision().getReflectionNotes().isEmpty()) {
                    sb.append(String.format(Locale.getDefault(), "  Notes: %s\n", record.getDecision().getReflectionNotes()));
                }
                sb.append("\n");
            }
        }
        
        sb.append(String.format(Locale.getDefault(), "Summary: %d good, %d bad, %d neutral decisions\n", goodCount, badCount, neutralCount));
        sb.append("\nPlease provide:\n");
        sb.append("1. Overall assessment of decision quality\n");
        sb.append("2. Patterns in good vs bad decisions\n");
        sb.append("3. Categories that need attention\n");
        sb.append("4. Specific actionable advice\n");
        sb.append("5. Encouragement based on progress\n");
        
        return sb.toString();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
