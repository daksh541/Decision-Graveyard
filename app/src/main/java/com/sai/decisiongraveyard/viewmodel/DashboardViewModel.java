package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.repository.UserProfileRepository;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private UserProfileRepository profileRepository;
    private ActivityRepository activityRepository;
    private DecisionRepository decisionRepository;

    private MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private MutableLiveData<Integer> decisionScore = new MutableLiveData<>(0);
    private MutableLiveData<Double> activityCompletionRate = new MutableLiveData<>(0.0);
    private MutableLiveData<Integer> todayActivitiesCount = new MutableLiveData<>(0);
    private MutableLiveData<Integer> todayDecisionsCount = new MutableLiveData<>(0);
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DashboardViewModel(Application application) {
        super(application);
        RepositoryProvider provider = RepositoryProvider.getInstance(application);
        profileRepository = provider.getUserProfileRepository();
        activityRepository = provider.getActivityRepository();
        decisionRepository = provider.getDecisionRepository();
        loadDashboardData();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<Integer> getDecisionScore() {
        return decisionScore;
    }

    public LiveData<Double> getActivityCompletionRate() {
        return activityCompletionRate;
    }

    public LiveData<Integer> getTodayActivitiesCount() {
        return todayActivitiesCount;
    }

    public LiveData<Integer> getTodayDecisionsCount() {
        return todayDecisionsCount;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadDashboardData() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        profileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                userProfile.setValue(profile);
                decisionScore.setValue((int) profile.getDecisionQualityRate());
                activityCompletionRate.setValue(profile.getActivityCompletionRate());
                loadTodayData();
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    private void loadTodayData() {
        long todayStart = getTodayStart();
        long todayEnd = getTodayEnd();

        new Thread(() -> {
            try {
                List<com.sai.decisiongraveyard.model.Activity> activities = activityRepository.getActivitiesForDay(todayStart, todayEnd);
                todayActivitiesCount.postValue(activities.size());
                checkLoadingComplete();
            } catch (Exception e) {
                errorMessage.postValue("Error loading activities: " + e.getMessage());
                checkLoadingComplete();
            }

            try {
                List<com.sai.decisiongraveyard.model.DecisionRecord> records = decisionRepository.getAllDecisionRecords();
                int todayCount = 0;
                for (com.sai.decisiongraveyard.model.DecisionRecord record : records) {
                    long decisionTime = record.getDecision().getDecisionTime();
                    if (decisionTime >= todayStart && decisionTime <= todayEnd) {
                        todayCount++;
                    }
                }
                todayDecisionsCount.postValue(todayCount);
                checkLoadingComplete();
            } catch (Exception e) {
                errorMessage.postValue("Error loading decisions: " + e.getMessage());
                checkLoadingComplete();
            }
        }).start();
    }

    private void checkLoadingComplete() {
        if (userProfile.getValue() != null && 
            (todayActivitiesCount.getValue() != null || todayActivitiesCount.hasObservers()) && 
            (todayDecisionsCount.getValue() != null || todayDecisionsCount.hasObservers())) {
            isLoading.postValue(false);
        }
    }

    private long getTodayStart() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getTodayEnd() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
}
