package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DecisionDetailViewModel extends AndroidViewModel {

    public static class ActionState {
        public final boolean saved;
        public final boolean deleted;
        public final String message;

        public ActionState(boolean saved, boolean deleted, String message) {
            this.saved = saved;
            this.deleted = deleted;
            this.message = message;
        }
    }

    private final DecisionRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<DecisionRecord> decisionRecord = new MutableLiveData<>();
    private final MutableLiveData<ActionState> actionState = new MutableLiveData<>();

    public DecisionDetailViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance(application).getDecisionRepository();
    }

    public LiveData<DecisionRecord> getDecisionRecord() {
        return decisionRecord;
    }

    public LiveData<ActionState> getActionState() {
        return actionState;
    }

    public void loadDecision(long decisionId) {
        executorService.execute(() -> {
            try {
                decisionRecord.postValue(repository.getDecisionRecord(decisionId));
            } catch (RuntimeException exception) {
                actionState.postValue(new ActionState(
                        false,
                        false,
                        exception.getMessage() == null
                                ? "Couldn't load this decision."
                                : exception.getMessage()
                ));
            }
        });
    }

    public void saveEvaluation(long decisionId, String outcome, String reflectionNotes) {
        repository.saveEvaluation(decisionId, outcome, reflectionNotes, new DecisionRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                actionState.postValue(new ActionState(true, false, "Evaluation saved."));
            }

            @Override
            public void onError(String error) {
                actionState.postValue(new ActionState(
                        false,
                        false,
                        error != null ? error : "Couldn't save the evaluation."
                ));
            }
        });
    }

    public void deleteDecision(long decisionId) {
        executorService.execute(() -> {
            try {
                repository.deleteDecision(decisionId);
                actionState.postValue(new ActionState(false, true, "Decision deleted."));
            } catch (RuntimeException exception) {
                actionState.postValue(new ActionState(
                        false,
                        false,
                        exception.getMessage() == null
                                ? "Couldn't delete the decision."
                                : exception.getMessage()
                ));
            }
        });
    }

    public void clearActionState() {
        actionState.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdownNow();
    }
}
