package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddDecisionViewModel extends AndroidViewModel {

    public static class SaveState {
        public final boolean success;
        public final String message;
        public final long decisionId;
        public final long evaluationTime;
        public final String title;

        public SaveState(boolean success, String message, long decisionId, long evaluationTime, String title) {
            this.success = success;
            this.message = message;
            this.decisionId = decisionId;
            this.evaluationTime = evaluationTime;
            this.title = title;
        }
    }

    private final DecisionRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<SaveState> saveState = new MutableLiveData<>();
    private final MutableLiveData<Long> customDateMillis = new MutableLiveData<>();

    public AddDecisionViewModel(@NonNull Application application) {
        super(application);
        DecisionRepository tempRepository;
        try {
            tempRepository = RepositoryProvider.getInstance(application).getDecisionRepository();
        } catch (RuntimeException exception) {
            tempRepository = null;
        }
        repository = tempRepository;
    }

    public LiveData<SaveState> getSaveState() {
        return saveState;
    }

    public LiveData<Long> getCustomDateMillis() {
        return customDateMillis;
    }

    public void setCustomDateMillis(Long millis) {
        customDateMillis.setValue(millis);
    }

    public void clearCustomDate() {
        customDateMillis.setValue(null);
    }

    public void saveDecision(String title, String description, String category, long evaluationTime) {
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedCategory = category == null ? "" : category.trim().toLowerCase();

        if (trimmedTitle.isEmpty()) {
            saveState.setValue(new SaveState(false, "Title is required.", -1L, -1L, null));
            return;
        }
        if (trimmedCategory.isEmpty()) {
            saveState.setValue(new SaveState(false, "Select a category.", -1L, -1L, null));
            return;
        }
        if (evaluationTime <= System.currentTimeMillis()) {
            saveState.setValue(new SaveState(false, "Choose a future review date.", -1L, -1L, null));
            return;
        }

        // Generate decisionId
        long decisionId = java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000_000_000L, Long.MAX_VALUE);

        // Check for similar decision asynchronously
        executorService.execute(() -> {
            try {
                if (repository == null) {
                    saveState.postValue(new SaveState(
                            false,
                            "Repository unavailable.",
                            -1L,
                            -1L,
                            null
                    ));
                    return;
                }
                if (repository.hasSimilarDecision(trimmedTitle, trimmedCategory, System.currentTimeMillis())) {
                    saveState.postValue(new SaveState(
                            false,
                            "A similar decision already exists in the last 24 hours.",
                            -1L,
                            -1L,
                            null
                    ));
                    return;
                }
                
                repository.createDecision(
                        decisionId,
                        trimmedTitle,
                        description == null ? "" : description.trim(),
                        trimmedCategory,
                        evaluationTime,
                        new DecisionRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                saveState.postValue(new SaveState(
                                        true,
                                        "Decision saved.",
                                        decisionId,
                                        evaluationTime,
                                        trimmedTitle
                                ));
                            }

                            @Override
                            public void onError(String error) {
                                saveState.postValue(new SaveState(
                                        false,
                                        error != null ? error : "Couldn't save your decision.",
                                        -1L,
                                        -1L,
                                        null
                                ));
                            }
                        }
                );
            } catch (Exception exception) {
                saveState.postValue(new SaveState(
                        false,
                        exception.getMessage() == null ? "Couldn't save your decision." : exception.getMessage(),
                        -1L,
                        -1L,
                        null
                ));
            }
        });
    }

    public void clearSaveState() {
        saveState.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdownNow();
    }
}
