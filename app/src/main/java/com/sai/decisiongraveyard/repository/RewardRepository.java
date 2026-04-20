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
import com.sai.decisiongraveyard.model.Reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RewardRepository {

    private static final String TAG = "RewardRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private UserProfileRepository userProfileRepository;

    public RewardRepository(Context context) {
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

    public interface RewardCallback {
        void onSuccess(Reward reward);
        void onError(String error);
    }

    public interface RewardListCallback {
        void onSuccess(List<Reward> rewards);
        void onError(String error);
    }

    public void initializeDefaultRewards(ActionCallback callback) {
        try {
            Log.d(TAG, "Initializing default rewards");
            
            List<Reward> defaultRewards = createDefaultRewards();
            
            for (Reward reward : defaultRewards) {
                rewardDocument(reward.getRewardId()).set(reward)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Reward initialized: " + reward.getRewardId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to initialize reward: " + e.getMessage()));
            }
            
            callback.onSuccess();
        } catch (Exception e) {
            Log.e(TAG, "Exception in initializeDefaultRewards: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    private List<Reward> createDefaultRewards() {
        List<Reward> rewards = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        // Streak Protection Rewards
        rewards.add(new Reward.Builder()
                .setRewardId("streak_shield_1")
                .setTitle("Streak Shield (1 Day)")
                .setDescription("Protect your streak for 1 day even if you miss activities")
                .setXpCost(50)
                .setIcon("🛡️")
                .setCategory("streak_protection")
                .setRewardType("consumable")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        rewards.add(new Reward.Builder()
                .setRewardId("streak_shield_3")
                .setTitle("Streak Shield (3 Days)")
                .setDescription("Protect your streak for 3 days even if you miss activities")
                .setXpCost(120)
                .setIcon("🛡️")
                .setCategory("streak_protection")
                .setRewardType("consumable")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        // XP Boost Rewards
        rewards.add(new Reward.Builder()
                .setRewardId("xp_boost_small")
                .setTitle("Small XP Boost")
                .setDescription("Get +25 XP immediately")
                .setXpCost(30)
                .setIcon("⚡")
                .setCategory("xp_boost")
                .setRewardType("instant")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        rewards.add(new Reward.Builder()
                .setRewardId("xp_boost_large")
                .setTitle("Large XP Boost")
                .setDescription("Get +100 XP immediately")
                .setXpCost(100)
                .setIcon("⚡")
                .setCategory("xp_boost")
                .setRewardType("instant")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        // Theme/Customization Rewards
        rewards.add(new Reward.Builder()
                .setRewardId("theme_dark_gold")
                .setTitle("Gold Dark Theme")
                .setDescription("Unlock premium gold dark theme")
                .setXpCost(200)
                .setIcon("🎨")
                .setCategory("theme")
                .setRewardType("permanent")
                .setOneTime(true)
                .setCreatedAt(now)
                .build());
        
        rewards.add(new Reward.Builder()
                .setRewardId("badge_elite")
                .setTitle("Elite Badge")
                .setDescription("Display elite badge on your profile")
                .setXpCost(150)
                .setIcon("🏅")
                .setCategory("badge")
                .setRewardType("permanent")
                .setOneTime(true)
                .setCreatedAt(now)
                .build());
        
        // Special Rewards
        rewards.add(new Reward.Builder()
                .setRewardId("second_chance")
                .setTitle("Second Chance")
                .setDescription("Undo one bad decision evaluation")
                .setXpCost(75)
                .setIcon("🔄")
                .setCategory("special")
                .setRewardType("consumable")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        rewards.add(new Reward.Builder()
                .setRewardId("focus_mode")
                .setTitle("Focus Mode")
                .setDescription("Enable focus mode for 24 hours - no distractions")
                .setXpCost(40)
                .setIcon("🎯")
                .setCategory("special")
                .setRewardType("temporary")
                .setOneTime(false)
                .setCreatedAt(now)
                .build());
        
        return rewards;
    }

    public void getAvailableRewards(RewardListCallback callback) {
        try {
            firestore.collection("rewards")
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Reward> rewards = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reward reward = doc.toObject(Reward.class);
                            if (reward != null) {
                                reward.setRewardId(doc.getId());
                                rewards.add(reward);
                            }
                        }
                        callback.onSuccess(rewards);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching rewards: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getAvailableRewards: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    public void getRewardsByCategory(String category, RewardListCallback callback) {
        try {
            firestore.collection("rewards")
                    .whereEqualTo("category", category)
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Reward> rewards = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reward reward = doc.toObject(Reward.class);
                            if (reward != null) {
                                reward.setRewardId(doc.getId());
                                rewards.add(reward);
                            }
                        }
                        callback.onSuccess(rewards);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching rewards by category: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getRewardsByCategory: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    public void getRewardById(String rewardId, RewardCallback callback) {
        try {
            rewardDocument(rewardId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Reward reward = documentSnapshot.toObject(Reward.class);
                            if (reward != null) {
                                reward.setRewardId(documentSnapshot.getId());
                                callback.onSuccess(reward);
                            } else {
                                callback.onError("Reward not found");
                            }
                        } else {
                            callback.onError("Reward not found");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching reward: " + e.getMessage(), e);
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getRewardById: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    public void redeemReward(String rewardId, ActionCallback callback) {
        try {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                callback.onError("Please log in to continue.");
                return;
            }

            String userId = user.getUid();

            // Get reward details
            getRewardById(rewardId, new RewardCallback() {
                @Override
                public void onSuccess(Reward reward) {
                    // Check if user has enough XP
                    if (userProfileRepository == null) {
                        callback.onError("User profile not available");
                        return;
                    }

                    userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
                        @Override
                        public void onSuccess(com.sai.decisiongraveyard.model.UserProfile profile) {
                            if (profile.getCurrentXP() < reward.getXpCost()) {
                                callback.onError("Not enough XP to redeem this reward");
                                return;
                            }

                            // Deduct XP
                            profile.removeXP(reward.getXpCost());
                            
                            // Update user profile
                            userProfileRepository.updateUserProfile(profile, new UserProfileRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    // Record redemption
                                    recordRedemption(userId, rewardId, reward, callback);
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onError("Failed to update profile: " + error);
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to get user profile: " + error);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in redeemReward: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    private void recordRedemption(String userId, String rewardId, Reward reward, ActionCallback callback) {
        try {
            String redemptionId = "redemption_" + System.currentTimeMillis();
            
            Map<String, Object> redemptionData = new java.util.LinkedHashMap<>();
            redemptionData.put("redemptionId", redemptionId);
            redemptionData.put("userId", userId);
            redemptionData.put("rewardId", rewardId);
            redemptionData.put("rewardTitle", reward.getTitle());
            redemptionData.put("xpCost", reward.getXpCost());
            redemptionData.put("rewardType", reward.getRewardType());
            redemptionData.put("redeemedAt", System.currentTimeMillis());

            firestore.collection("redemptions")
                    .document(redemptionId)
                    .set(redemptionData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Redemption recorded: " + redemptionId);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to record redemption: " + e.getMessage(), e);
                        callback.onSuccess(); // Still consider successful even if recording fails
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in recordRedemption: " + e.getMessage(), e);
            callback.onSuccess(); // Still consider successful
        }
    }

    public void getUserRedemptions(int limit, RewardListCallback callback) {
        String userId = requireUserId();

        firestore.collection("redemptions")
                .whereEqualTo("userId", userId)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Reward> rewards = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String rewardId = doc.getString("rewardId");
                        if (rewardId != null) {
                            getRewardById(rewardId, new RewardCallback() {
                                @Override
                                public void onSuccess(Reward reward) {
                                    rewards.add(reward);
                                    if (rewards.size() == querySnapshot.size()) {
                                        callback.onSuccess(rewards);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Error fetching redeemed reward: " + error);
                                }
                            });
                        }
                    }
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess(rewards);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching redemptions: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    private String requireUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Please log in to continue.");
        }
        return currentUser.getUid();
    }

    private com.google.firebase.firestore.DocumentReference rewardDocument(String rewardId) {
        return firestore.collection("rewards").document(rewardId);
    }
}
