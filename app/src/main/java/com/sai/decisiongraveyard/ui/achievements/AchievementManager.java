package com.sai.decisiongraveyard.ui.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.app.AlertDialog;

import com.sai.decisiongraveyard.R;

import java.util.HashSet;
import java.util.Set;

public class AchievementManager {

    private static final String PREFS_NAME = "achievements_prefs";
    private static final String KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements";

    public enum Achievement {
        FIRST_DECISION("first_decision", "First Decision", "🎯", "You logged your first decision!"),
        FIRST_EVALUATION("first_evaluation", "First Evaluation", "✅", "You evaluated your first decision!"),
        THREE_DAY_STREAK("three_day_streak", "3-Day Streak", "🔥", "You maintained a 3-day discipline streak!"),
        SEVEN_DAY_STREAK("seven_day_streak", "7-Day Streak", "⚡", "Amazing! 7-day discipline streak!"),
        TEN_DECISIONS("ten_decisions", "10 Decisions", "📊", "You've logged 10 decisions!"),
        FIFTY_DECISIONS("fifty_decisions", "50 Decisions", "📈", "You've logged 50 decisions!"),
        PERFECT_DAY("perfect_day", "Perfect Day", "🌟", "You completed all activities today!"),
        LEVEL_UP("level_up", "Level Up", "🎖️", "You reached a new level!");

        private final String id;
        private final String title;
        private final String icon;
        private final String description;

        Achievement(String id, String title, String icon, String description) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.description = description;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final Set<String> unlockedAchievements;

    public AchievementManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.unlockedAchievements = new HashSet<>(prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, new HashSet<>()));
    }

    public void unlockAchievement(Achievement achievement) {
        if (!unlockedAchievements.contains(achievement.getId())) {
            unlockedAchievements.add(achievement.getId());
            prefs.edit().putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, unlockedAchievements).apply();
            showAchievementDialog(achievement);
        }
    }

    public boolean isUnlocked(Achievement achievement) {
        return unlockedAchievements.contains(achievement.getId());
    }

    private void showAchievementDialog(Achievement achievement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Achievement Unlocked!");
        
        StringBuilder message = new StringBuilder();
        message.append(achievement.getIcon()).append(" ").append(achievement.getTitle()).append("\n\n");
        message.append(achievement.getDescription());
        
        builder.setMessage(message.toString());
        builder.setPositiveButton("Awesome", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Add celebration animation
        celebrateAchievement(dialog);
    }

    private void celebrateAchievement(AlertDialog dialog) {
        View decorView = dialog.getWindow().getDecorView();
        
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(500);
        decorView.startAnimation(scaleAnimation);
    }

    public void checkFirstDecision(int decisionCount) {
        if (decisionCount >= 1) {
            unlockAchievement(Achievement.FIRST_DECISION);
        }
        if (decisionCount >= 10) {
            unlockAchievement(Achievement.TEN_DECISIONS);
        }
        if (decisionCount >= 50) {
            unlockAchievement(Achievement.FIFTY_DECISIONS);
        }
    }

    public void checkFirstEvaluation(int evaluationCount) {
        if (evaluationCount >= 1) {
            unlockAchievement(Achievement.FIRST_EVALUATION);
        }
    }

    public void checkStreak(int streakDays) {
        if (streakDays >= 3) {
            unlockAchievement(Achievement.THREE_DAY_STREAK);
        }
        if (streakDays >= 7) {
            unlockAchievement(Achievement.SEVEN_DAY_STREAK);
        }
    }

    public void checkPerfectDay(boolean allActivitiesCompleted) {
        if (allActivitiesCompleted) {
            unlockAchievement(Achievement.PERFECT_DAY);
        }
    }

    public void checkLevelUp(int newLevel) {
        if (newLevel > 1) {
            unlockAchievement(Achievement.LEVEL_UP);
        }
    }
}
