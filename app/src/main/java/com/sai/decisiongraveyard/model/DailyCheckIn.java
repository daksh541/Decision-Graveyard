package com.sai.decisiongraveyard.model;

import com.google.firebase.Timestamp;

public class DailyCheckIn {

    private String checkInId;
    private String userId;
    private long date;
    private boolean decisionsMade;
    private boolean activitiesCompleted;
    private boolean submitted;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public DailyCheckIn() {
    }

    public DailyCheckIn(String userId, long date) {
        this.userId = userId;
        this.date = date;
        this.decisionsMade = false;
        this.activitiesCompleted = false;
        this.submitted = false;
        this.createdAt = new Timestamp(System.currentTimeMillis() / 1000, 0);
        this.updatedAt = this.createdAt;
    }

    public String getCheckInId() {
        return checkInId;
    }

    public void setCheckInId(String checkInId) {
        this.checkInId = checkInId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean isDecisionsMade() {
        return decisionsMade;
    }

    public void setDecisionsMade(boolean decisionsMade) {
        this.decisionsMade = decisionsMade;
    }

    public boolean isActivitiesCompleted() {
        return activitiesCompleted;
    }

    public void setActivitiesCompleted(boolean activitiesCompleted) {
        this.activitiesCompleted = activitiesCompleted;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
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

    public boolean isComplete() {
        return decisionsMade && activitiesCompleted;
    }

    @Override
    public String toString() {
        return "DailyCheckIn{" +
                "checkInId='" + checkInId + '\'' +
                ", userId='" + userId + '\'' +
                ", date=" + date +
                ", decisionsMade=" + decisionsMade +
                ", submitted=" + submitted +
                ", activitiesCompleted=" + activitiesCompleted +
                '}';
    }
}
