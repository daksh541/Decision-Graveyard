package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    public static class HomeScreenState {
        public final List<DecisionRecord> filteredRecords;
        public final int totalCount;
        public final int readyCount;
        public final int upcomingCount;
        public final String identityLabel;
        public final int qualityScore;

        public HomeScreenState(List<DecisionRecord> filteredRecords, int totalCount,
                               int readyCount, int upcomingCount, String identityLabel, int qualityScore) {
            this.filteredRecords = filteredRecords;
            this.totalCount = totalCount;
            this.readyCount = readyCount;
            this.upcomingCount = upcomingCount;
            this.identityLabel = identityLabel;
            this.qualityScore = qualityScore;
        }
    }

    private final DecisionRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<HomeScreenState> screenState = new MutableLiveData<>();

    private String statusFilter = "all";
    private String categoryFilter = "All";
    private List<DecisionRecord> cachedRecords = new ArrayList<>();
    private ListenerRegistration registration;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance(application).getDecisionRepository();
        startListening();
    }

    private void startListening() {
        if (registration != null) {
            registration.remove();
        }
        registration = repository.listenToDecisions(new DecisionRepository.DecisionRecordsListener() {
            @Override
            public void onChanged(@NonNull List<DecisionRecord> records) {
                Log.d(TAG, "Decisions updated: " + records.size() + " items");
                cachedRecords = records;
                postFilteredState();
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "Error listening to decisions: " + message);
            }
        });
    }

    public LiveData<HomeScreenState> getScreenState() {
        return screenState;
    }

    public void refresh() {
        if (registration == null) {
            startListening();
        }
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
        postFilteredState();
    }

    public void setCategoryFilter(String categoryFilter) {
        this.categoryFilter = categoryFilter;
        postFilteredState();
    }

    private void postFilteredState() {
        List<DecisionRecord> filtered = new ArrayList<>();
        int readyCount = 0;
        int upcomingCount = 0;
        int goodCount = 0;
        int totalEvaluated = 0;

        for (DecisionRecord record : cachedRecords) {
            if (record.isReadyForReview()) {
                readyCount++;
            } else if (record.isUpcoming()) {
                upcomingCount++;
            }

            if (record.isEvaluated()) {
                totalEvaluated++;
                if ("good".equals(record.getEvaluation().getOutcome())) {
                    goodCount++;
                }
            }

            if (!matchesStatus(record) || !matchesCategory(record)) {
                continue;
            }
            filtered.add(record);
        }

        // Calculate identity label and quality score
        int qualityScore = totalEvaluated == 0 ? 0 : (int) Math.round(goodCount * 100.0 / totalEvaluated);
        String identityLabel = calculateIdentityLabel(qualityScore);

        Log.d(TAG, "Posting screen state: filteredCount=" + filtered.size() + ", totalCount=" + cachedRecords.size());
        screenState.postValue(new HomeScreenState(filtered, cachedRecords.size(), readyCount, upcomingCount, identityLabel, qualityScore));
    }

    private String calculateIdentityLabel(int score) {
        if (score >= 80) return "Level 4: Disciplined";
        if (score >= 60) return "Level 3: Controlled";
        if (score >= 40) return "Level 2: Aware";
        return "Level 1: Chaotic";
    }

    private boolean matchesStatus(DecisionRecord record) {
        if ("ready".equals(statusFilter)) {
            return record.isReadyForReview();
        }
        if ("reviewed".equals(statusFilter)) {
            return record.isEvaluated();
        }
        return true;
    }

    private boolean matchesCategory(DecisionRecord record) {
        return "All".equals(categoryFilter)
                || categoryFilter.equalsIgnoreCase(record.getDecision().getCategory());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
        }
        executorService.shutdownNow();
    }
}
