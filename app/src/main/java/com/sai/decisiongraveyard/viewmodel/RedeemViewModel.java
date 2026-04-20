package com.sai.decisiongraveyard.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sai.decisiongraveyard.manager.RedemptionManager;
import com.sai.decisiongraveyard.model.Reward;
import com.sai.decisiongraveyard.model.UserProfile;
import com.sai.decisiongraveyard.repository.RewardRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;

import java.util.ArrayList;
import java.util.List;

public class RedeemViewModel extends AndroidViewModel {

    private static final String TAG = "RedeemViewModel";

    private final RewardRepository rewardRepository;
    private final UserProfileRepository userProfileRepository;
    private final RedemptionManager redemptionManager;

    private final MutableLiveData<List<Reward>> availableRewards = new MutableLiveData<>();
    private final MutableLiveData<List<Reward>> streakProtectionRewards = new MutableLiveData<>();
    private final MutableLiveData<List<Reward>> xpBoostRewards = new MutableLiveData<>();
    private final MutableLiveData<List<Reward>> themeRewards = new MutableLiveData<>();
    private final MutableLiveData<List<Reward>> specialRewards = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public RedeemViewModel(@NonNull Application application) {
        super(application);
        this.rewardRepository = new RewardRepository(application);
        this.userProfileRepository = new UserProfileRepository(application);
        this.rewardRepository.setUserProfileRepository(userProfileRepository);
        this.redemptionManager = new RedemptionManager(application, rewardRepository, userProfileRepository);
    }

    public LiveData<List<Reward>> getAvailableRewards() {
        return availableRewards;
    }

    public LiveData<List<Reward>> getStreakProtectionRewards() {
        return streakProtectionRewards;
    }

    public LiveData<List<Reward>> getXpBoostRewards() {
        return xpBoostRewards;
    }

    public LiveData<List<Reward>> getThemeRewards() {
        return themeRewards;
    }

    public LiveData<List<Reward>> getSpecialRewards() {
        return specialRewards;
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadAllRewards() {
        isLoading.setValue(true);
        
        rewardRepository.getAvailableRewards(new RewardRepository.RewardListCallback() {
            @Override
            public void onSuccess(List<Reward> rewards) {
                availableRewards.setValue(rewards);
                categorizeRewards(rewards);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Failed to load rewards: " + error);
                isLoading.setValue(false);
            }
        });
    }

    public void loadRewardsByCategory(String category) {
        isLoading.setValue(true);
        
        rewardRepository.getRewardsByCategory(category, new RewardRepository.RewardListCallback() {
            @Override
            public void onSuccess(List<Reward> rewards) {
                availableRewards.setValue(rewards);
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Failed to load rewards: " + error);
                isLoading.setValue(false);
            }
        });
    }

    private void categorizeRewards(List<Reward> rewards) {
        List<Reward> streakProtection = new ArrayList<>();
        List<Reward> xpBoost = new ArrayList<>();
        List<Reward> theme = new ArrayList<>();
        List<Reward> special = new ArrayList<>();

        for (Reward reward : rewards) {
            switch (reward.getCategory()) {
                case "streak_protection":
                    streakProtection.add(reward);
                    break;
                case "xp_boost":
                    xpBoost.add(reward);
                    break;
                case "theme":
                case "badge":
                    theme.add(reward);
                    break;
                case "special":
                    special.add(reward);
                    break;
            }
        }

        streakProtectionRewards.setValue(streakProtection);
        xpBoostRewards.setValue(xpBoost);
        themeRewards.setValue(theme);
        specialRewards.setValue(special);
    }

    public void loadUserProfile() {
        userProfileRepository.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                userProfile.setValue(profile);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Failed to load profile: " + error);
            }
        });
    }

    public void redeemReward(Reward reward) {
        redemptionManager.redeemReward(reward, new RedemptionManager.RedemptionCallback() {
            @Override
            public void onSuccess() {
                successMessage.setValue("Reward redeemed successfully!");
                loadUserProfile(); // Refresh XP balance
                loadAllRewards(); // Refresh rewards list
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public void showRedemptionConfirmation(Reward reward) {
        redemptionManager.showRedemptionConfirmation(reward, new RedemptionManager.RedemptionCallback() {
            @Override
            public void onSuccess() {
                successMessage.setValue("Reward redeemed successfully!");
                loadUserProfile();
                loadAllRewards();
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public void initializeDefaultRewards() {
        isLoading.setValue(true);
        rewardRepository.initializeDefaultRewards(new RewardRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                successMessage.setValue("Default rewards initialized");
                loadAllRewards();
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue("Failed to initialize rewards: " + error);
                isLoading.setValue(false);
            }
        });
    }

    public boolean hasConsumableReward(String rewardId) {
        return redemptionManager.hasConsumableReward(rewardId);
    }

    public int getConsumableRewardCount(String rewardId) {
        return redemptionManager.getConsumableRewardCount(rewardId);
    }

    public boolean hasPermanentReward(String rewardId) {
        return redemptionManager.hasPermanentReward(rewardId);
    }

    public boolean hasTemporaryReward(String rewardId) {
        return redemptionManager.hasTemporaryReward(rewardId);
    }

    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
}
