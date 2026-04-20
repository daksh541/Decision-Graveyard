package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.DailyCheckIn;
import com.sai.decisiongraveyard.repository.DailyCheckInRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.List;

public class DailyCheckInViewModel extends AndroidViewModel {

    private DailyCheckInRepository repository;
    private MutableLiveData<DailyCheckIn> todayCheckIn = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>(false);

    public DailyCheckInViewModel(Application application) {
        super(application);
        RepositoryProvider provider = RepositoryProvider.getInstance(application);
        repository = provider.getDailyCheckInRepository();
        loadTodayCheckIn();
    }

    public LiveData<DailyCheckIn> getTodayCheckIn() {
        return todayCheckIn;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getActionSuccess() {
        return actionSuccess;
    }

    public void loadTodayCheckIn() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getTodayCheckIn(new DailyCheckInRepository.CheckInCallback() {
            @Override
            public void onSuccess(DailyCheckIn checkIn) {
                isLoading.setValue(false);
                if (checkIn == null) {
                    repository.createTodayCheckIn(new DailyCheckInRepository.CheckInCallback() {
                        @Override
                        public void onSuccess(DailyCheckIn newCheckIn) {
                            todayCheckIn.setValue(newCheckIn);
                        }

                        @Override
                        public void onError(String error) {
                            errorMessage.setValue(error);
                        }
                    });
                } else {
                    todayCheckIn.setValue(checkIn);
                }
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void markDecisionsMade() {
        DailyCheckIn checkIn = todayCheckIn.getValue();
        if (checkIn == null || checkIn.getCheckInId() == null) {
            errorMessage.setValue("No check-in found");
            return;
        }

        isLoading.setValue(true);
        repository.markDecisionsMade(checkIn.getCheckInId(), new DailyCheckInRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                checkIn.setDecisionsMade(true);
                todayCheckIn.setValue(checkIn);
                actionSuccess.setValue(true);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void markActivitiesCompleted() {
        DailyCheckIn checkIn = todayCheckIn.getValue();
        if (checkIn == null || checkIn.getCheckInId() == null) {
            errorMessage.setValue("No check-in found");
            return;
        }

        isLoading.setValue(true);
        repository.markActivitiesCompleted(checkIn.getCheckInId(), new DailyCheckInRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                checkIn.setActivitiesCompleted(true);
                todayCheckIn.setValue(checkIn);
                actionSuccess.setValue(true);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void clearActionSuccess() {
        actionSuccess.setValue(false);
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
