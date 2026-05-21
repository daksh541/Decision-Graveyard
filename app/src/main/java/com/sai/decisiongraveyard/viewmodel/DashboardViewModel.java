package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;
import com.sai.decisiongraveyard.repository.UserProfileRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends AndroidViewModel {

    private final UserProfileRepository profileRepository;
    private final ActivityRepository activityRepository;
    private final DecisionRepository decisionRepository;

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Integer> decisionScore = new MutableLiveData<>(0);
    private final MutableLiveData<Double> activityCompletionRate = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> todayCompletedActivities = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> todayMissedActivities = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> todayRiskyDecisions = new MutableLiveData<>(0);
    private final MutableLiveData<AnalyticsSnapshot> analyticsSnapshot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

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

    public LiveData<Integer> getTodayCompletedActivities() {
        return todayCompletedActivities;
    }

    public LiveData<Integer> getTodayMissedActivities() {
        return todayMissedActivities;
    }

    public LiveData<Integer> getTodayRiskyDecisions() {
        return todayRiskyDecisions;
    }

    public LiveData<AnalyticsSnapshot> getAnalyticsSnapshot() {
        return analyticsSnapshot;
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
            int completedCount = 0;
            int missedCount = 0;
            int riskyDecisionCount = 0;

            try {
                List<com.sai.decisiongraveyard.model.Activity> activities =
                        activityRepository.getActivitiesForDay(todayStart, todayEnd);
                for (com.sai.decisiongraveyard.model.Activity activity : activities) {
                    if (activity.isCompleted()) {
                        completedCount++;
                    } else if ("missed".equals(activity.getStatus())) {
                        missedCount++;
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("Error loading activities: " + e.getMessage());
            }

            try {
                List<DecisionRecord> records = decisionRepository.getAllDecisionRecords();
                Map<String, Integer> regretRates = buildCategoryRegretRates(records);
                for (DecisionRecord record : records) {
                    long decisionTime = record.getDecision().getDecisionTime();
                    if (decisionTime >= todayStart && decisionTime <= todayEnd && isRiskyDecision(record, regretRates)) {
                        riskyDecisionCount++;
                    }
                }
                analyticsSnapshot.postValue(decisionRepository.getAnalyticsSnapshot(false));
            } catch (Exception e) {
                errorMessage.postValue("Error loading decisions: " + e.getMessage());
            } finally {
                todayCompletedActivities.postValue(completedCount);
                todayMissedActivities.postValue(missedCount);
                todayRiskyDecisions.postValue(riskyDecisionCount);
                isLoading.postValue(false);
            }
        }).start();
    }

    private Map<String, Integer> buildCategoryRegretRates(List<DecisionRecord> records) {
        Map<String, Integer> evaluatedCounts = new HashMap<>();
        Map<String, Integer> badCounts = new HashMap<>();

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) {
                continue;
            }

            String category = record.getDecision().getCategory();
            if (category == null) {
                continue;
            }

            evaluatedCounts.put(category, evaluatedCounts.getOrDefault(category, 0) + 1);
            if ("bad".equalsIgnoreCase(record.getEvaluation().getOutcome())) {
                badCounts.put(category, badCounts.getOrDefault(category, 0) + 1);
            }
        }

        Map<String, Integer> regretRates = new HashMap<>();
        for (Map.Entry<String, Integer> entry : evaluatedCounts.entrySet()) {
            int badCount = badCounts.getOrDefault(entry.getKey(), 0);
            regretRates.put(entry.getKey(), (int) Math.round((badCount * 100.0) / entry.getValue()));
        }
        return regretRates;
    }

    private boolean isRiskyDecision(DecisionRecord record, Map<String, Integer> regretRates) {
        String category = record.getDecision().getCategory();
        int decisionHour = record.getDecision().getDecisionHour();
        int regretRate = category == null ? 0 : regretRates.getOrDefault(category, 0);
        return decisionHour >= 21 || regretRate >= 60;
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
