package com.sai.decisiongraveyard.model;

import com.google.firebase.Timestamp;

public class UserProfile {

    private String userId;
    private String levelName;
    private int currentLevel;
    private int currentXP;
    private int xpToNextLevel;
    private int dailyDisciplineStreak;
    private int missedDayStreak;
    private int totalGoodDecisions;
    private int totalBadDecisions;
    private int totalCompletedActivities;
    private int totalMissedActivities;
    private double activityCompletionRate;
    private double progressPercentage;
    private double decisionQualityRate;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public UserProfile() {
        this.currentLevel = 1;
        this.currentXP = 0;
        this.xpToNextLevel = 100;
        this.dailyDisciplineStreak = 0;
        this.missedDayStreak = 0;
        this.totalGoodDecisions = 0;
        this.totalBadDecisions = 0;
        this.totalCompletedActivities = 0;
        this.totalMissedActivities = 0;
    }

    public UserProfile(String userId) {
        this();
        this.userId = userId;
        this.createdAt = new Timestamp(System.currentTimeMillis() / 1000, 0);
        this.updatedAt = this.createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public void setActivityCompletionRate(double activityCompletionRate) {
        this.activityCompletionRate = activityCompletionRate;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public void setDecisionQualityRate(double decisionQualityRate) {
        this.decisionQualityRate = decisionQualityRate;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getCurrentXP() {
        return currentXP;
    }

    public void setCurrentXP(int currentXP) {
        this.currentXP = currentXP;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    public void setXpToNextLevel(int xpToNextLevel) {
        this.xpToNextLevel = xpToNextLevel;
    }

    public int getDailyDisciplineStreak() {
        return dailyDisciplineStreak;
    }

    public void setDailyDisciplineStreak(int dailyDisciplineStreak) {
        this.dailyDisciplineStreak = dailyDisciplineStreak;
    }

    public int getMissedDayStreak() {
        return missedDayStreak;
    }

    public void setMissedDayStreak(int missedDayStreak) {
        this.missedDayStreak = missedDayStreak;
    }

    public int getTotalGoodDecisions() {
        return totalGoodDecisions;
    }

    public void setTotalGoodDecisions(int totalGoodDecisions) {
        this.totalGoodDecisions = totalGoodDecisions;
    }

    public int getTotalBadDecisions() {
        return totalBadDecisions;
    }

    public void setTotalBadDecisions(int totalBadDecisions) {
        this.totalBadDecisions = totalBadDecisions;
    }

    public int getTotalCompletedActivities() {
        return totalCompletedActivities;
    }

    public void setTotalCompletedActivities(int totalCompletedActivities) {
        this.totalCompletedActivities = totalCompletedActivities;
    }

    public int getTotalMissedActivities() {
        return totalMissedActivities;
    }

    public void setTotalMissedActivities(int totalMissedActivities) {
        this.totalMissedActivities = totalMissedActivities;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addXP(int amount) {
        this.currentXP += amount;
        checkLevelUp();
    }

    public void removeXP(int amount) {
        this.currentXP = Math.max(0, this.currentXP - amount);
    }

    private void checkLevelUp() {
        while (this.currentXP >= this.xpToNextLevel && this.currentLevel < 5) {
            this.currentXP -= this.xpToNextLevel;
            this.currentLevel++;
            this.xpToNextLevel = calculateXPForLevel(this.currentLevel + 1);
        }
    }

    private int calculateXPForLevel(int level) {
        switch (level) {
            case 1: return 100;
            case 2: return 250;
            case 3: return 500;
            case 4: return 1000;
            case 5: return Integer.MAX_VALUE;
            default: return 100;
        }
    }

    public String getLevelName() {
        switch (this.currentLevel) {
            case 1: return "Chaotic";
            case 2: return "Aware";
            case 3: return "Controlled";
            case 4: return "Disciplined";
            case 5: return "Optimized";
            default: return "Chaotic";
        }
    }

    public double getProgressPercentage() {
        return (double) this.currentXP / this.xpToNextLevel * 100;
    }

    public double getDecisionQualityRate() {
        int total = totalGoodDecisions + totalBadDecisions;
        if (total == 0) return 0;
        return (double) totalGoodDecisions / total * 100;
    }

    public double getActivityCompletionRate() {
        int total = totalCompletedActivities + totalMissedActivities;
        if (total == 0) return 0;
        return (double) totalCompletedActivities / total * 100;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "userId='" + userId + '\'' +
                ", currentLevel=" + currentLevel +
                ", currentXP=" + currentXP +
                ", xpToNextLevel=" + xpToNextLevel +
                ", dailyDisciplineStreak=" + dailyDisciplineStreak +
                ", missedDayStreak=" + missedDayStreak +
                '}';
    }
}
