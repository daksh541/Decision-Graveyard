package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.UserProfile;

import java.util.concurrent.ExecutionException;

public class UserProfileRepository {

    private static final String TAG = "UserProfileRepository";
    private static final String COLLECTION_NAME = "userProfiles";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Context context;
    private final MutableLiveData<Long> dataChangedTrigger = new MutableLiveData<>();

    public UserProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface ProfileCallback {
        void onSuccess(UserProfile profile);
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

    private DocumentReference profileDocument(String userId) {
        return db.collection(COLLECTION_NAME).document(userId);
    }

    public LiveData<Long> getDataChangedTrigger() {
        return dataChangedTrigger;
    }

    public void getUserProfile(ProfileCallback callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage() == null ? "User not authenticated" : exception.getMessage());
            return;
        }

        profileDocument(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                    if (profile == null) {
                        UserProfile newProfile = new UserProfile(userId);
                        profileDocument(userId).set(newProfile)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(newProfile))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating new profile", e);
                                    callback.onError(e.getMessage());
                                });
                    } else {
                        profile.setUserId(userId);
                        callback.onSuccess(profile);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateUserProfile(UserProfile profile, ActionCallback callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage() == null ? "User not authenticated" : exception.getMessage());
            return;
        }
        profile.setUserId(userId);
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(Timestamp.now());
        }
        profile.setUpdatedAt(Timestamp.now());

        profileDocument(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    dataChangedTrigger.postValue(System.currentTimeMillis());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    callback.onError(e.getMessage());
                });
    }

    public void addGoodDecision(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setTotalGoodDecisions(profile.getTotalGoodDecisions() + 1);
                profile.addXP(10);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void addBadDecision(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setTotalBadDecisions(profile.getTotalBadDecisions() + 1);
                profile.removeXP(5);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void addCompletedActivity(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setTotalCompletedActivities(profile.getTotalCompletedActivities() + 1);
                profile.addXP(15);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void addMissedActivity(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setTotalMissedActivities(profile.getTotalMissedActivities() + 1);
                profile.removeXP(10);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void incrementDailyDisciplineStreak(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setDailyDisciplineStreak(profile.getDailyDisciplineStreak() + 1);
                profile.setMissedDayStreak(0);
                profile.addXP(20);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void incrementMissedDayStreak(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setMissedDayStreak(profile.getMissedDayStreak() + 1);
                profile.setDailyDisciplineStreak(0);
                updateUserProfile(profile, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void resetDisciplineStreak(ActionCallback callback) {
        getUserProfile(new ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setDailyDisciplineStreak(0);
                updateUserProfile(profile, callback);
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
