package com.sai.decisiongraveyard.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;
import com.sai.decisiongraveyard.repository.UserPreferencesRepository;

public class BehavioralTriggerWorker extends Worker {

    private static final String TAG = "BehavioralTriggerWorker";

    public BehavioralTriggerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        try {
            checkForBehavioralTriggers(context);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error checking behavioral triggers: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    private void checkForBehavioralTriggers(Context context) {
        UserProfileRepository profileRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getUserProfileRepository();
        UserPreferencesRepository preferencesRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getUserPreferencesRepository();
        
        profileRepo.getUserProfile(new UserProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserProfile profile) {
                preferencesRepo.getUserPreferences(new UserPreferencesRepository.PreferencesCallback() {
                    @Override
                    public void onSuccess(com.sai.decisiongraveyard.model.UserPreferences preferences) {
                        boolean isBrutal = preferences.isBrutalMode();
                        
                        // Check for level up
                        checkLevelUp(context, profile, isBrutal);
                        
                        // Check for streak milestones
                        checkStreakMilestones(context, profile, isBrutal);
                        
                        // Check for pattern warnings
                        checkPatternWarnings(context, profile, isBrutal);
                        
                        // Check for motivation triggers
                        checkMotivationTriggers(context, profile, isBrutal);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error getting preferences: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting user profile: " + error);
            }
        });
    }

    private void checkLevelUp(Context context, com.sai.decisiongraveyard.model.UserProfile profile, boolean isBrutal) {
        // Check if user just leveled up (this would be tracked in the profile)
        // For now, we'll send a notification if they're close to leveling up
        double progress = profile.getProgressPercentage();
        
        if (progress >= 80 && progress < 100) {
            String message = isBrutal 
                    ? "You're almost at Level " + (profile.getCurrentLevel() + 1) + ". Don't stop now."
                    : "You're close to Level " + (profile.getCurrentLevel() + 1) + "! Keep going!";
            
            SmartNotificationScheduler.showBehavioralTriggerNotification(context, "level_up", message);
        }
    }

    private void checkStreakMilestones(Context context, com.sai.decisiongraveyard.model.UserProfile profile, boolean isBrutal) {
        int streak = profile.getDailyDisciplineStreak();
        
        if (streak == 7 || streak == 14 || streak == 30 || streak == 100) {
            String message = isBrutal
                    ? streak + " day streak. Prove it's not luck."
                    : "Amazing! " + streak + " day streak. You're building a habit!";
            
            SmartNotificationScheduler.showBehavioralTriggerNotification(context, "streak_milestone", message);
        }
    }

    private void checkPatternWarnings(Context context, com.sai.decisiongraveyard.model.UserProfile profile, boolean isBrutal) {
        int missedStreak = profile.getMissedDayStreak();
        
        if (missedStreak >= 3) {
            String message = isBrutal
                    ? missedStreak + " days missed. This is becoming a pattern. Break it."
                    : "You've missed " + missedStreak + " days in a row. Let's get back on track.";
            
            SmartNotificationScheduler.showBehavioralTriggerNotification(context, "pattern_warning", message);
        }
    }

    private void checkMotivationTriggers(Context context, com.sai.decisiongraveyard.model.UserProfile profile, boolean isBrutal) {
        // Send motivation if decision quality is low
        double decisionRate = profile.getDecisionQualityRate();
        
        if (decisionRate < 40 && decisionRate > 0) {
            String message = isBrutal
                    ? "Your decision quality is at " + String.format("%.0f%%", decisionRate) + ". Fix it."
                    : "Your decision quality is " + String.format("%.0f%%", decisionRate) + ". Take more time before deciding.";
            
            SmartNotificationScheduler.showBehavioralTriggerNotification(context, "motivation", message);
        }
    }
}
