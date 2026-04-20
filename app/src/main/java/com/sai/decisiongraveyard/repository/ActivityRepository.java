package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.model.PatternTracker;
import com.sai.decisiongraveyard.notifications.NotificationScheduler;
import com.sai.decisiongraveyard.notifications.PreMissReminderScheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ExecutionException;

public class ActivityRepository {

    public interface ActivityListListener {
        void onChanged(@NonNull List<Activity> activities);
        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    private static final String TAG = "ActivityRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final MutableLiveData<Long> dataChangedTrigger = new MutableLiveData<>();

    private final Context context;
    private UserProfileRepository userProfileRepository;

    public ActivityRepository(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void setUserProfileRepository(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public LiveData<Long> getDataChangedTrigger() {
        return dataChangedTrigger;
    }

    public void createActivity(String title, String category, long scheduledTime, ActionCallback callback) {
        try {
            Log.d(TAG, "createActivity called with title: " + title);
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Log.e(TAG, "Save failed: No authenticated user.");
                callback.onError("Please log in to continue.");
                return;
            }
            String userId = user.getUid();
            Log.d(TAG, "User authenticated with userId: " + userId);

            long activityId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, Long.MAX_VALUE);
            Log.d(TAG, "Generated activityId: " + activityId);

            Activity activity = new Activity(title, category, scheduledTime, userId);
            activity.setActivityId(activityId);

            Log.d(TAG, "Saving activity: title=" + title + ", userId=" + userId + ", object=" + activity);

            activityDocument(activityId).set(activity)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Activity saved successfully with ID: " + activityId);
                        dataChangedTrigger.postValue(activityId);
                        
                        // Schedule pre-miss reminder (10 minutes before scheduled time)
                        PreMissReminderScheduler.schedulePreMissReminder(context, activity);
                        
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save activity: " + e.getMessage(), e);
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save activity");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in createActivity: " + e.getMessage(), e);
            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save activity");
        }
    }

    public void markAsCompleted(long activityId, ActionCallback callback) {
        try {
            Log.d(TAG, "markAsCompleted called for activityId: " + activityId);
            
            Map<String, Object> updates = new java.util.LinkedHashMap<>();
            updates.put("completed", true);
            updates.put("completedTime", System.currentTimeMillis());

            activityDocument(activityId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Activity marked as completed: " + activityId);
                        dataChangedTrigger.postValue(activityId);
                        
                        // Cancel pre-miss reminder since activity is completed
                        PreMissReminderScheduler.cancelPreMissReminder(context, activityId);
                        
                        // Award XP for completing activity
                        if (userProfileRepository != null) {
                            userProfileRepository.addCompletedActivity(new UserProfileRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onSuccess(); // Still succeed even if XP fails
                                }
                            });
                        } else {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to mark activity as completed: " + e.getMessage(), e);
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update activity");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in markAsCompleted: " + e.getMessage(), e);
            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update activity");
        }
    }

    public void deleteActivity(long activityId, ActionCallback callback) {
        try {
            Log.d(TAG, "deleteActivity called for activityId: " + activityId);
            
            activityDocument(activityId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Activity deleted: " + activityId);
                        dataChangedTrigger.postValue(activityId);
                        
                        // Cancel pre-miss reminder since activity is deleted
                        PreMissReminderScheduler.cancelPreMissReminder(context, activityId);
                        
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete activity: " + e.getMessage(), e);
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete activity");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in deleteActivity: " + e.getMessage(), e);
            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete activity");
        }
    }

    public List<Activity> getAllActivities() {
        String userId = requireUserId();
        Log.d(TAG, "Fetching all activities for userId: " + userId);

        QuerySnapshot snapshot = await(
                firestore.collection("activities")
                        .whereEqualTo("userId", userId)
                        .get()
        );

        Log.d(TAG, "Fetched " + snapshot.size() + " documents");
        return mapActivities(snapshot);
    }

    public List<Activity> getActivitiesForDay(long dayStartMillis, long dayEndMillis) {
        String userId = requireUserId();
        Log.d(TAG, "Fetching activities for day: " + dayStartMillis + " to " + dayEndMillis);

        QuerySnapshot snapshot = await(
                firestore.collection("activities")
                        .whereEqualTo("userId", userId)
                        .whereGreaterThanOrEqualTo("scheduledTime", dayStartMillis)
                        .whereLessThan("scheduledTime", dayEndMillis)
                        .get()
        );

        Log.d(TAG, "Fetched " + snapshot.size() + " activities for the day");
        return mapActivities(snapshot);
    }

    public List<Activity> getMissedActivities() {
        String userId = requireUserId();
        long now = System.currentTimeMillis();
        Log.d(TAG, "Fetching missed activities for userId: " + userId);

        // Fetch all activities for user and filter in-memory to avoid composite index requirement
        QuerySnapshot snapshot = await(
                firestore.collection("activities")
                        .whereEqualTo("userId", userId)
                        .get()
        );

        List<Activity> allActivities = mapActivities(snapshot);
        List<Activity> missedActivities = new ArrayList<>();
        for (Activity activity : allActivities) {
            if (activity.getScheduledTime() < now && !activity.isCompleted()) {
                missedActivities.add(activity);
            }
        }

        Log.d(TAG, "Filtered " + missedActivities.size() + " missed activities from " + allActivities.size() + " total");
        return missedActivities;
    }

    public PatternTracker detectFailurePattern() {
        String userId = requireUserId();
        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000);

        QuerySnapshot snapshot = await(
                firestore.collection("activities")
                        .whereEqualTo("userId", userId)
                        .whereGreaterThanOrEqualTo("scheduledTime", sevenDaysAgo)
                        .whereLessThan("scheduledTime", now)
                        .get()
        );

        List<Activity> activities = mapActivities(snapshot);
        List<String> consecutiveMisses = new ArrayList<>();
        String mostMissedCategory = "";
        int totalMisses = 0;
        java.util.Map<String, Integer> categoryMissCount = new java.util.LinkedHashMap<>();

        int consecutiveCount = 0;
        for (Activity activity : activities) {
            if (!activity.isCompleted()) {
                consecutiveCount++;
                consecutiveMisses.add(activity.getTitle());
                totalMisses++;

                String category = activity.getCategory();
                categoryMissCount.put(category, categoryMissCount.getOrDefault(category, 0) + 1);
            } else {
                consecutiveCount = 0;
            }
        }

        // Find most missed category
        int maxMisses = 0;
        for (java.util.Map.Entry<String, Integer> entry : categoryMissCount.entrySet()) {
            if (entry.getValue() > maxMisses) {
                maxMisses = entry.getValue();
                mostMissedCategory = entry.getKey();
            }
        }

        boolean isPatternDetected = consecutiveCount >= 3;
        String patternMessage = "";
        int recoveryActivitiesNeeded = 0;

        if (isPatternDetected) {
            patternMessage = "This is not a bad day. This is a pattern.";
            recoveryActivitiesNeeded = Math.min(consecutiveCount, 5);
        } else if (consecutiveCount == 2) {
            patternMessage = "Two misses in a row. Don't make it three.";
            recoveryActivitiesNeeded = 2;
        }

        return new PatternTracker.Builder()
                .setConsecutiveMisses(consecutiveMisses)
                .setTotalMisses(totalMisses)
                .setMostMissedCategory(mostMissedCategory)
                .setPatternDetected(isPatternDetected)
                .setPatternMessage(patternMessage)
                .setRecoveryActivitiesNeeded(recoveryActivitiesNeeded)
                .build();
    }

    public ListenerRegistration listenToActivities(@NonNull ActivityListListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Listen failed: No authenticated user.");
            listener.onError("Please log in to see your activities.");
            return null;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Setting up snapshot listener for userId: " + userId);

        return firestore.collection("activities")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error: " + error.getMessage() + ", code: " + error.getCode());
                        listener.onError(error.getMessage() == null
                                ? "Couldn't load activities right now."
                                : error.getMessage());
                        return;
                    }
                    if (snapshots == null) {
                        Log.w(TAG, "Snapshot is null - returning empty list");
                        listener.onChanged(new ArrayList<>());
                        return;
                    }

                    Log.d(TAG, "Snapshot update: " + snapshots.size() + " documents");

                    List<Activity> activities = mapActivities(snapshots);
                    Log.d(TAG, "Mapped " + activities.size() + " activities");
                    listener.onChanged(activities);
                });
    }

    public Activity getActivity(long activityId) {
        DocumentSnapshot snapshot = await(activityDocument(activityId).get());
        if (!snapshot.exists()) {
            Log.w(TAG, "Activity document not found: " + activityId);
            return null;
        }

        Activity activity = snapshot.toObject(Activity.class);
        if (activity == null) {
            Log.w(TAG, "Failed to deserialize activity: " + activityId);
            return null;
        }

        String currentUserId = requireUserId();
        String activityUserId = activity.getUserId();

        if (activityUserId == null) {
            Log.w(TAG, "Activity has null userId: " + activityId);
            return null;
        }

        if (!currentUserId.equals(activityUserId)) {
            Log.w(TAG, "Activity belongs to different user: " + activityId);
            return null;
        }

        if (activity.getActivityId() == 0L) {
            try {
                activity.setActivityId(Long.parseLong(snapshot.getId()));
            } catch (NumberFormatException ignored) {
                Log.w(TAG, "Skipping activity with invalid ID: " + snapshot.getId());
                return null;
            }
        }

        return activity;
    }

    private List<Activity> mapActivities(@NonNull QuerySnapshot snapshot) {
        List<Activity> activities = new ArrayList<>();

        for (QueryDocumentSnapshot document : snapshot) {
            Activity activity = document.toObject(Activity.class);
            if (activity == null) {
                Log.w(TAG, "Skipping null activity for document: " + document.getId());
                continue;
            }

            String docUserId = activity.getUserId();
            if (docUserId == null) {
                Log.w(TAG, "Skipping activity with null userId: " + document.getId());
                continue;
            }

            if (activity.getActivityId() == 0L) {
                try {
                    activity.setActivityId(Long.parseLong(document.getId()));
                } catch (NumberFormatException ignored) {
                    Log.w(TAG, "Skipping activity with invalid ID: " + document.getId());
                    continue;
                }
            }
            activities.add(activity);
        }

        activities.sort(Comparator.comparingLong(Activity::getScheduledTime));
        return activities;
    }

    private String requireUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Please log in to continue.");
        }
        return currentUser.getUid();
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            throw new RuntimeException(
                    cause != null && cause.getMessage() != null
                            ? cause.getMessage()
                            : "We couldn't complete that action.",
                    cause != null ? cause : exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The request was interrupted.", exception);
        }
    }

    private com.google.firebase.firestore.DocumentReference activityDocument(long activityId) {
        return firestore.collection("activities").document(String.valueOf(activityId));
    }
}
