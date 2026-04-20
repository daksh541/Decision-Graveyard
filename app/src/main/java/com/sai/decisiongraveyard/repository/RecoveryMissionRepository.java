package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.RecoveryMission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class RecoveryMissionRepository {

    private static final String TAG = "RecoveryMissionRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private UserProfileRepository userProfileRepository;

    public RecoveryMissionRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void setUserProfileRepository(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public void createRecoveryMission(int activitiesToComplete, ActionCallback callback) {
        try {
            Log.d(TAG, "createRecoveryMission called");
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Log.e(TAG, "No authenticated user");
                callback.onError("Please log in to continue.");
                return;
            }

            String userId = user.getUid();
            String missionId = "mission_" + ThreadLocalRandom.current().nextInt(1_000_000);
            
            // Mission deadline: end of today
            long now = System.currentTimeMillis();
            long endOfDay = now + (24 * 60 * 60 * 1000);

            // Calculate XP reward based on difficulty
            int xpReward = activitiesToComplete * 25;

            RecoveryMission mission = new RecoveryMission.Builder()
                    .setMissionId(missionId)
                    .setTitle("Regain Control")
                    .setDescription("Complete " + activitiesToComplete + " activities today to break the pattern.")
                    .setActivitiesToComplete(activitiesToComplete)
                    .setActivitiesCompleted(0)
                    .setDeadline(endOfDay)
                    .setCompleted(false)
                    .setUserId(userId)
                    .setXpReward(xpReward)
                    .setResetsStreak(true)
                    .setMissionType("pattern_break")
                    .setCreatedAt(now)
                    .build();

            missionDocument(missionId).set(mission)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Recovery mission created: " + missionId);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create recovery mission: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in createRecoveryMission: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    public void updateMissionProgress(String missionId, int activitiesCompleted, ActionCallback callback) {
        try {
            Map<String, Object> updates = new java.util.LinkedHashMap<>();
            updates.put("activitiesCompleted", activitiesCompleted);

            missionDocument(missionId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Mission progress updated: " + missionId);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update mission: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in updateMissionProgress: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    public void completeMission(String missionId, ActionCallback callback) {
        try {
            Map<String, Object> updates = new java.util.LinkedHashMap<>();
            updates.put("isCompleted", true);
            updates.put("completedAt", System.currentTimeMillis());

            missionDocument(missionId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Mission completed: " + missionId);
                        
                        // Get mission details to apply rewards
                        RecoveryMission mission = getMissionById(missionId);
                        if (mission != null) {
                            applyMissionRewards(mission, callback);
                        } else {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to complete mission: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in completeMission: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    private void applyMissionRewards(RecoveryMission mission, ActionCallback callback) {
        if (userProfileRepository == null) {
            callback.onSuccess();
            return;
        }
        
        // Add XP reward
        userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserProfile profile) {
                profile.addXP(mission.getXpReward());
                
                // Reset missed day streak if mission is configured to do so
                if (mission.isResetsStreak()) {
                    profile.setMissedDayStreak(0);
                }
                
                userProfileRepository.updateUserProfile(profile, new UserProfileRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Mission rewards applied: XP=" + mission.getXpReward());
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Failed to apply rewards: " + error);
                        callback.onSuccess(); // Still consider mission complete even if reward fails
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get user profile for rewards: " + error);
                callback.onSuccess(); // Still consider mission complete
            }
        });
    }

    public RecoveryMission getActiveMission() {
        String userId = requireUserId();
        long now = System.currentTimeMillis();

        try {
            QuerySnapshot snapshot = Tasks.await(
                    firestore.collection("recovery_missions")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("isCompleted", false)
                            .whereGreaterThan("deadline", now)
                            .get()
            );

            if (snapshot.isEmpty()) {
                return null;
            }

            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            return doc.toObject(RecoveryMission.class);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching active mission: " + e.getMessage(), e);
            return null;
        }
    }

    public RecoveryMission getMissionById(String missionId) {
        try {
            DocumentSnapshot doc = Tasks.await(missionDocument(missionId).get());
            if (!doc.exists()) {
                return null;
            }
            return doc.toObject(RecoveryMission.class);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching mission by ID: " + e.getMessage(), e);
            return null;
        }
    }

    public void getCompletedMissions(int limit, MissionListCallback callback) {
        String userId = requireUserId();

        firestore.collection("recovery_missions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", true)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RecoveryMission> missions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        RecoveryMission mission = doc.toObject(RecoveryMission.class);
                        if (mission != null) {
                            mission.setMissionId(doc.getId());
                            missions.add(mission);
                        }
                    }
                    // Sort descending by completedAt
                    missions.sort((m1, m2) -> Long.compare(m2.getCompletedAt(), m1.getCompletedAt()));
                    callback.onSuccess(missions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching completed missions: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public interface MissionListCallback {
        void onSuccess(List<RecoveryMission> missions);
        void onError(String error);
    }

    private String requireUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Please log in to continue.");
        }
        return currentUser.getUid();
    }

    private com.google.firebase.firestore.DocumentReference missionDocument(String missionId) {
        return firestore.collection("recovery_missions").document(missionId);
    }
}
