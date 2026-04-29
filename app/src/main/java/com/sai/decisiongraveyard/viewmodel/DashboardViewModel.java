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
    private MutableLiveData<String> todayActivitiesCount = new MutableLiveData<>("0/0");
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

    public LiveData<String> getTodayActivitiesCount() {
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
            int todayActivityCount = 0;
            int todayCompletedCount = 0;
            int todayDecisionCount = 0;

            try {
                List<com.sai.decisiongraveyard.model.Activity> activities = activityRepository.getActivitiesForDay(todayStart, todayEnd);
                todayActivityCount = activities.size();
                for (com.sai.decisiongraveyard.model.Activity activity : activities) {
                    if (activity.isCompleted()) {
                        todayCompletedCount++;
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("Error loading activities: " + e.getMessage());
            }

            try {
                List<com.sai.decisiongraveyard.model.DecisionRecord> records = decisionRepository.getAllDecisionRecords();
                for (com.sai.decisiongraveyard.model.DecisionRecord record : records) {
                    long decisionTime = record.getDecision().getDecisionTime();
                    if (decisionTime >= todayStart && decisionTime <= todayEnd) {
                        todayDecisionCount++;
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("Error loading decisions: " + e.getMessage());
            } finally {
                todayActivitiesCount.postValue(todayCompletedCount + "/" + todayActivityCount);
                todayDecisionsCount.postValue(todayDecisionCount);
                isLoading.postValue(false);
            }
        }).start();
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
