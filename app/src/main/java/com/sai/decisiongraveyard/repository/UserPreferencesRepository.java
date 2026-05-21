package com.sai.decisiongraveyard.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sai.decisiongraveyard.model.UserPreferences;

import java.util.concurrent.ExecutionException;

public class UserPreferencesRepository {

    private static final String TAG = "UserPreferencesRepository";
    private static final String COLLECTION_NAME = "userPreferences";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public UserPreferencesRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface PreferencesCallback {
        void onSuccess(UserPreferences preferences);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public String requireUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return auth.getCurrentUser().getUid();
    }

    private DocumentReference preferencesDocument(String userId) {
        return db.collection(COLLECTION_NAME).document(userId);
    }

    public void getUserPreferences(PreferencesCallback callback) {
        String userId = requireUserId();

        preferencesDocument(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserPreferences preferences = documentSnapshot.toObject(UserPreferences.class);
                    if (preferences == null) {
                        UserPreferences newPreferences = new UserPreferences(userId);
                        preferencesDocument(userId).set(newPreferences)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(newPreferences))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating new preferences", e);
                                    callback.onError(e.getMessage());
                                });
                    } else {
                        preferences.setUserId(userId);
                        callback.onSuccess(preferences);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user preferences", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateUserPreferences(UserPreferences preferences, ActionCallback callback) {
        String userId = requireUserId();
        preferences.setUserId(userId);
        if (preferences.getCreatedAt() == null) {
            preferences.setCreatedAt(Timestamp.now());
        }
        preferences.setUpdatedAt(Timestamp.now());

        preferencesDocument(userId)
                .set(preferences)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user preferences", e);
                    callback.onError(e.getMessage());
                });
    }

    public void setNotificationIntensity(String intensity, ActionCallback callback) {
        getUserPreferences(new PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences preferences) {
                preferences.setNotificationIntensity(intensity);
                updateUserPreferences(preferences, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void setGoals(java.util.List<String> goals, ActionCallback callback) {
        getUserPreferences(new PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences preferences) {
                preferences.setSelectedGoals(goals);
                updateUserPreferences(preferences, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void setPreMissReminderMinutes(int minutes, ActionCallback callback) {
        getUserPreferences(new PreferencesCallback() {
            @Override
            public void onSuccess(UserPreferences preferences) {
                preferences.setPreMissReminderMinutes(minutes);
                updateUserPreferences(preferences, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
