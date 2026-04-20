package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InsightsViewModel extends AndroidViewModel {

    private final DecisionRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<AnalyticsSnapshot> analyticsSnapshot = new MutableLiveData<>();
    private final MutableLiveData<Integer> streakCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> badStreakCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> brutalMode = new MutableLiveData<>(false);

    public InsightsViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance(application).getDecisionRepository();
    }

    public LiveData<AnalyticsSnapshot> getAnalyticsSnapshot() {
        return analyticsSnapshot;
    }

    public LiveData<Integer> getStreakCount() {
        return streakCount;
    }

    public LiveData<Integer> getBadStreakCount() {
        return badStreakCount;
    }

    public LiveData<Boolean> getBrutalMode() {
        return brutalMode;
    }

    public void setBrutalMode(boolean enabled) {
        brutalMode.setValue(enabled);
        refresh();
    }

    public void refresh() {
        executorService.execute(() -> {
            try {
                Boolean isBrutal = brutalMode.getValue();
                analyticsSnapshot.postValue(repository.getAnalyticsSnapshot(isBrutal != null && isBrutal));
                streakCount.postValue(calculateStreak());
                badStreakCount.postValue(calculateBadStreak());
            } catch (RuntimeException ignored) {
                analyticsSnapshot.postValue(new AnalyticsSnapshot(
                        0, 0, 0, 0, 0, 0, 0,
                        java.util.Collections.emptyList(),
                        java.util.Collections.emptyList()
                ));
                streakCount.postValue(0);
                badStreakCount.postValue(0);
            }
        });
    }

    private int calculateStreak() {
        List<DecisionRecord> records = repository.getAllDecisionRecords();
        int streak = 0;
        long lastEvaluatedTime = 0;

        // Sort by evaluation time ascending (oldest first)
        records.sort((a, b) -> {
            Long timeA = a.isEvaluated() ? a.getEvaluation().getEvaluatedAt() : Long.MAX_VALUE;
            Long timeB = b.isEvaluated() ? b.getEvaluation().getEvaluatedAt() : Long.MAX_VALUE;
            return timeA.compareTo(timeB);
        });

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) continue;

            String outcome = record.getEvaluation().getOutcome();
            long evalTime = record.getEvaluation().getEvaluatedAt();

            if ("good".equals(outcome)) {
                // Check if consecutive (within 24 hours of last good decision)
                if (lastEvaluatedTime == 0 || Math.abs(evalTime - lastEvaluatedTime) <= 24 * 60 * 60 * 1000) {
                    streak++;
                    lastEvaluatedTime = evalTime;
                } else if (evalTime > lastEvaluatedTime) {
                    // This is a newer decision, continue streak
                    streak++;
                    lastEvaluatedTime = evalTime;
                } else {
                    break; // Streak broken (gap too large)
                }
            } else {
                break; // Streak broken by bad/neutral decision
            }
        }

        return streak;
    }

    private int calculateBadStreak() {
        List<DecisionRecord> records = repository.getAllDecisionRecords();
        int badStreak = 0;
        long lastEvaluatedTime = 0;

        // Sort by evaluation time ascending (oldest first)
        records.sort((a, b) -> {
            Long timeA = a.isEvaluated() ? a.getEvaluation().getEvaluatedAt() : Long.MAX_VALUE;
            Long timeB = b.isEvaluated() ? b.getEvaluation().getEvaluatedAt() : Long.MAX_VALUE;
            return timeA.compareTo(timeB);
        });

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) continue;

            String outcome = record.getEvaluation().getOutcome();
            long evalTime = record.getEvaluation().getEvaluatedAt();

            if ("bad".equals(outcome)) {
                // Check if consecutive (within 24 hours of last bad decision)
                if (lastEvaluatedTime == 0 || Math.abs(evalTime - lastEvaluatedTime) <= 24 * 60 * 60 * 1000) {
                    badStreak++;
                    lastEvaluatedTime = evalTime;
                } else if (evalTime > lastEvaluatedTime) {
                    // This is a newer decision, continue streak
                    badStreak++;
                    lastEvaluatedTime = evalTime;
                } else {
                    break; // Streak broken (gap too large)
                }
            } else {
                break; // Streak broken by good/neutral decision
            }
        }

        return badStreak;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdownNow();
    }
}
