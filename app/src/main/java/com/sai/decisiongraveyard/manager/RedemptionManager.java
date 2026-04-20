package com.sai.decisiongraveyard.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Reward;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.RewardRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;

import java.util.HashMap;
import java.util.Map;

public class RedemptionManager {

    private static final String TAG = "RedemptionManager";
    private static final String PREFS_NAME = "redemption_prefs";
    private static final String KEY_ACTIVE_REWARDS = "active_rewards";
    private static final String KEY_REDEEMED_COUNT = "redeemed_count";

    private final Context context;
    private final SharedPreferences prefs;
    private final RewardRepository rewardRepository;
    private final UserProfileRepository userProfileRepository;
    private final Map<String, Integer> redeemedCounts;

    public RedemptionManager(Context context, RewardRepository rewardRepository, 
                           UserProfileRepository userProfileRepository) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.rewardRepository = rewardRepository;
        this.userProfileRepository = userProfileRepository;
        this.redeemedCounts = loadRedeemedCounts();
    }

    public void redeemReward(Reward reward, RedemptionCallback callback) {
        try {
            Log.d(TAG, "Redeeming reward: " + reward.getRewardId());

            // Check if one-time reward already redeemed
            if (reward.isOneTime() && hasRedeemedBefore(reward.getRewardId())) {
                callback.onError("You have already redeemed this reward");
                return;
            }

            // Get user profile to check XP
            userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
                @Override
                public void onSuccess(UserProfile profile) {
                    if (profile.getCurrentXP() < reward.getXpCost()) {
                        callback.onError("Not enough XP. Required: " + reward.getXpCost() + 
                                        ", Available: " + profile.getCurrentXP());
                        return;
                    }

                    // Proceed with redemption
                    rewardRepository.redeemReward(reward.getRewardId(), 
                            new RewardRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    // Record redemption
                                    recordRedemption(reward.getRewardId());
                                    
                                    // Apply reward effect
                                    applyRewardEffect(reward, profile, callback);
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Redemption failed: " + error);
                                    callback.onError(error);
                                }
                            });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to get user profile: " + error);
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in redeemReward: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }

    private void applyRewardEffect(Reward reward, UserProfile profile, RedemptionCallback callback) {
        try {
            switch (reward.getRewardType()) {
                case "instant":
                    // Instant rewards like XP boost are handled during redemption
                    showSuccessDialog(reward, "Reward redeemed successfully!");
                    callback.onSuccess();
                    break;

                case "consumable":
                    // Store for later use
                    activateConsumableReward(reward);
                    showSuccessDialog(reward, "Reward added to your inventory!");
                    callback.onSuccess();
                    break;

                case "permanent":
                    // Permanent unlock
                    activatePermanentReward(reward);
                    showSuccessDialog(reward, "Reward unlocked permanently!");
                    callback.onSuccess();
                    break;

                case "temporary":
                    // Temporary effect
                    activateTemporaryReward(reward);
                    showSuccessDialog(reward, "Reward activated for 24 hours!");
                    callback.onSuccess();
                    break;

                default:
                    showSuccessDialog(reward, "Reward redeemed successfully!");
                    callback.onSuccess();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in applyRewardEffect: " + e.getMessage(), e);
            callback.onError("Failed to apply reward effect");
        }
    }

    private void activateConsumableReward(Reward reward) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = "consumable_" + reward.getRewardId();
        int currentCount = prefs.getInt(key, 0);
        editor.putInt(key, currentCount + 1);
        editor.apply();
        Log.d(TAG, "Activated consumable reward: " + reward.getRewardId() + ", Count: " + (currentCount + 1));
    }

    private void activatePermanentReward(Reward reward) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = "permanent_" + reward.getRewardId();
        editor.putBoolean(key, true);
        editor.apply();
        Log.d(TAG, "Activated permanent reward: " + reward.getRewardId());
    }

    private void activateTemporaryReward(Reward reward) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = "temporary_" + reward.getRewardId();
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        editor.putLong(key, expiryTime);
        editor.apply();
        Log.d(TAG, "Activated temporary reward: " + reward.getRewardId() + ", Expires: " + expiryTime);
    }

    public boolean hasConsumableReward(String rewardId) {
        String key = "consumable_" + rewardId;
        return prefs.getInt(key, 0) > 0;
    }

    public int getConsumableRewardCount(String rewardId) {
        String key = "consumable_" + rewardId;
        return prefs.getInt(key, 0);
    }

    public void useConsumableReward(String rewardId) {
        String key = "consumable_" + rewardId;
        int currentCount = prefs.getInt(key, 0);
        if (currentCount > 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(key, currentCount - 1);
            editor.apply();
            Log.d(TAG, "Used consumable reward: " + rewardId);
        }
    }

    public boolean hasPermanentReward(String rewardId) {
        String key = "permanent_" + rewardId;
        return prefs.getBoolean(key, false);
    }

    public boolean hasTemporaryReward(String rewardId) {
        String key = "temporary_" + rewardId;
        long expiryTime = prefs.getLong(key, 0);
        return expiryTime > System.currentTimeMillis();
    }

    private void recordRedemption(String rewardId) {
        int count = redeemedCounts.getOrDefault(rewardId, 0);
        redeemedCounts.put(rewardId, count + 1);
        saveRedeemedCounts();
    }

    private boolean hasRedeemedBefore(String rewardId) {
        return redeemedCounts.containsKey(rewardId) && redeemedCounts.get(rewardId) > 0;
    }

    private Map<String, Integer> loadRedeemedCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String savedData = prefs.getString(KEY_REDEEMED_COUNT, "");
        if (!savedData.isEmpty()) {
            String[] entries = savedData.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    counts.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        }
        return counts;
    }

    private void saveRedeemedCounts() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : redeemedCounts.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        prefs.edit().putString(KEY_REDEEMED_COUNT, sb.toString()).apply();
    }

    private void showSuccessDialog(Reward reward, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🎉 Reward Redeemed!");
        
        StringBuilder sb = new StringBuilder();
        sb.append(reward.getIcon()).append(" ").append(reward.getTitle()).append("\n\n");
        sb.append(message);
        
        builder.setMessage(sb.toString());
        builder.setPositiveButton("Great", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showRedemptionConfirmation(Reward reward, RedemptionCallback callback) {
        userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                boolean canAfford = profile.getCurrentXP() >= reward.getXpCost();
                
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Redeem Reward?");
                
                StringBuilder sb = new StringBuilder();
                sb.append(reward.getIcon()).append(" ").append(reward.getTitle()).append("\n\n");
                sb.append(reward.getDescription()).append("\n\n");
                sb.append("Cost: ").append(reward.getXpCost()).append(" XP\n");
                sb.append("Your XP: ").append(profile.getCurrentXP()).append("\n\n");
                
                if (!canAfford) {
                    sb.append("❌ Not enough XP!");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                } else {
                    sb.append("✓ You can afford this!");
                    builder.setPositiveButton("Redeem", (dialog, which) -> {
                        redeemReward(reward, callback);
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                }
                
                builder.setMessage(sb.toString());
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to check XP balance");
            }
        });
    }

    public interface RedemptionCallback {
        void onSuccess();
        void onError(String error);
    }
}
