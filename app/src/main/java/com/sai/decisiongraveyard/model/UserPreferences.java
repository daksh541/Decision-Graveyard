package com.sai.decisiongraveyard.model;

import com.google.firebase.Timestamp;

import java.util.List;

public class UserPreferences {

    private String preferencesId;
    private String userId;
    private boolean brutalMode; // Added to handle Firestore deserialization
    private List<String> selectedGoals;
    private String notificationIntensity;
    private boolean dailySummaryEnabled;
    private boolean weeklyReportEnabled;
    private int preMissReminderMinutes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public UserPreferences() {
        this.notificationIntensity = "normal";
        this.dailySummaryEnabled = true;
        this.weeklyReportEnabled = true;
        this.preMissReminderMinutes = 10;
    }

    public UserPreferences(String userId) {
        this();
        this.userId = userId;
        this.createdAt = new Timestamp(System.currentTimeMillis() / 1000, 0);
        this.updatedAt = this.createdAt;
    }

    public String getPreferencesId() {
        return preferencesId;
    }

    public void setPreferencesId(String preferencesId) {
        this.preferencesId = preferencesId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBrutalMode(boolean brutalMode) {
        this.brutalMode = brutalMode;
    }

    public List<String> getSelectedGoals() {
        return selectedGoals;
    }

    public void setSelectedGoals(List<String> selectedGoals) {
        this.selectedGoals = selectedGoals;
    }

    public String getNotificationIntensity() {
        return notificationIntensity;
    }

    public void setNotificationIntensity(String notificationIntensity) {
        this.notificationIntensity = notificationIntensity;
    }

    public boolean isDailySummaryEnabled() {
        return dailySummaryEnabled;
    }

    public void setDailySummaryEnabled(boolean dailySummaryEnabled) {
        this.dailySummaryEnabled = dailySummaryEnabled;
    }

    public boolean isWeeklyReportEnabled() {
        return weeklyReportEnabled;
    }

    public void setWeeklyReportEnabled(boolean weeklyReportEnabled) {
        this.weeklyReportEnabled = weeklyReportEnabled;
    }

    public int getPreMissReminderMinutes() {
        return preMissReminderMinutes;
    }

    public void setPreMissReminderMinutes(int preMissReminderMinutes) {
        this.preMissReminderMinutes = preMissReminderMinutes;
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

    public boolean isBrutalMode() {
        return "brutal".equalsIgnoreCase(notificationIntensity);
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "preferencesId='" + preferencesId + '\'' +
                ", userId='" + userId + '\'' +
                ", notificationIntensity='" + notificationIntensity + '\'' +
                ", dailySummaryEnabled=" + dailySummaryEnabled +
                ", weeklyReportEnabled=" + weeklyReportEnabled +
                ", preMissReminderMinutes=" + preMissReminderMinutes +
                '}';
    }
}
