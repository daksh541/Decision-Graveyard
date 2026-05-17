package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.RepositoryProvider;

import java.util.ArrayList;
import java.util.List;

public class ActivityViewModel extends AndroidViewModel {

    public static class SaveState {
        public final boolean success;
        public final String message;

        public SaveState(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final ActivityRepository repository;
    private final MutableLiveData<SaveState> saveState = new MutableLiveData<>();
    private final MutableLiveData<SaveState> actionState = new MutableLiveData<>();
    private final MutableLiveData<List<Activity>> activities = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusFilter = new MutableLiveData<>("all");
    private final List<Activity> cachedActivities = new ArrayList<>();
    private ListenerRegistration registration;

    public ActivityViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance(application).getActivityRepository();
        startListening();
    }

    public LiveData<SaveState> getSaveState() {
        return saveState;
    }

    public LiveData<List<Activity>> getActivities() {
        return activities;
    }

    public LiveData<SaveState> getActionState() {
        return actionState;
    }

    public LiveData<String> getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String filter) {
        statusFilter.setValue(filter);
        postFilteredActivities();
    }

    public void refresh() {
        if (registration == null) {
            startListening();
            return;
        }
        postFilteredActivities();
    }

    private void startListening() {
        if (registration != null) {
            registration.remove();
        }

        registration = repository.listenToActivities(new ActivityRepository.ActivityListListener() {
            @Override
            public void onChanged(@NonNull List<Activity> activityList) {
                cachedActivities.clear();
                cachedActivities.addAll(activityList);
                postFilteredActivities();
            }

            @Override
            public void onError(@NonNull String message) {
                android.util.Log.e("ActivityViewModel", "Error loading activities: " + message);
            }
        });
    }

    private void postFilteredActivities() {
        activities.postValue(filterActivities(new ArrayList<>(cachedActivities), statusFilter.getValue()));
    }

    private List<Activity> filterActivities(List<Activity> allActivities, String filter) {
        if (filter == null || "all".equals(filter)) {
            return allActivities;
        }

        List<Activity> filtered = new ArrayList<>();
        for (Activity activity : allActivities) {
            String status = activity.getStatus();
            if (status != null && filter.equals(status)) {
                filtered.add(activity);
            }
        }

        return filtered;
    }

    public void saveActivity(String title, String category, long scheduledTime) {
        android.util.Log.d("ActivityViewModel", "saveActivity called");
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedCategory = category == null ? "" : category.trim().toLowerCase();

        android.util.Log.d("ActivityViewModel", "Trimmed values - title: " + trimmedTitle + ", category: " + trimmedCategory);

        if (trimmedTitle.isEmpty()) {
            android.util.Log.w("ActivityViewModel", "Title is empty");
            saveState.postValue(new SaveState(false, "Title is required."));
            return;
        }
        if (trimmedCategory.isEmpty()) {
            android.util.Log.w("ActivityViewModel", "Category is empty");
            saveState.postValue(new SaveState(false, "Select a category."));
            return;
        }
        if (scheduledTime <= System.currentTimeMillis()) {
            android.util.Log.w("ActivityViewModel", "Scheduled time is in the past");
            saveState.postValue(new SaveState(false, "Choose a future time."));
            return;
        }

        repository.createActivity(trimmedTitle, trimmedCategory, scheduledTime, new ActivityRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("ActivityViewModel", "Activity created successfully");
                saveState.postValue(new SaveState(true, "Activity saved."));
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("ActivityViewModel", "Error during save: " + error);
                saveState.postValue(new SaveState(false, error != null ? error : "Couldn't save your activity."));
            }
        });
    }

    public void markAsCompleted(long activityId) {
        repository.markAsCompleted(activityId, new ActivityRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("ActivityViewModel", "Activity marked as completed: " + activityId);
                boolean updated = false;
                for (Activity activity : cachedActivities) {
                    if (activity.getActivityId() == activityId) {
                        activity.setCompleted(true);
                        activity.setCompletedTime(System.currentTimeMillis());
                        updated = true;
                        break;
                    }
                }
                if (updated) {
                    postFilteredActivities();
                } else {
                    refresh();
                }
                actionState.postValue(new SaveState(true, "Activity marked as completed"));
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("ActivityViewModel", "Error marking as completed: " + error);
                actionState.postValue(new SaveState(false, error != null ? error : "Couldn't mark activity as completed."));
            }
        });
    }

    public void deleteActivity(long activityId) {
        repository.deleteActivity(activityId, new ActivityRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("ActivityViewModel", "Activity deleted: " + activityId);
                cachedActivities.removeIf(activity -> activity.getActivityId() == activityId);
                postFilteredActivities();
                actionState.postValue(new SaveState(true, "Activity deleted"));
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("ActivityViewModel", "Error deleting activity: " + error);
                actionState.postValue(new SaveState(false, error != null ? error : "Couldn't delete activity."));
            }
        });
    }

    public void clearSaveState() {
        saveState.setValue(null);
    }

    public void clearActionState() {
        actionState.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
