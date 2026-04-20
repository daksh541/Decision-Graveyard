package com.sai.decisiongraveyard.model;

public class RecoveryMission {

    private String missionId;
    private String title;
    private String description;
    private int activitiesToComplete;
    private int activitiesCompleted;
    private long deadline;
    private boolean isCompleted;
    private String userId;
    private int xpReward;
    private boolean resetsStreak;
    private String missionType;
    private long createdAt;
    private long completedAt;

    public RecoveryMission() {
        this.missionType = "pattern_break";
        this.resetsStreak = true;
        this.xpReward = 50;
    }

    public RecoveryMission(String missionId, String title, String description, 
                          int activitiesToComplete, int activitiesCompleted, 
                          long deadline, boolean isCompleted, String userId) {
        this();
        this.missionId = missionId;
        this.title = title;
        this.description = description;
        this.activitiesToComplete = activitiesToComplete;
        this.activitiesCompleted = activitiesCompleted;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.userId = userId;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getActivitiesToComplete() {
        return activitiesToComplete;
    }

    public void setActivitiesToComplete(int activitiesToComplete) {
        this.activitiesToComplete = activitiesToComplete;
    }

    public int getActivitiesCompleted() {
        return activitiesCompleted;
    }

    public void setActivitiesCompleted(int activitiesCompleted) {
        this.activitiesCompleted = activitiesCompleted;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public boolean isResetsStreak() {
        return resetsStreak;
    }

    public void setResetsStreak(boolean resetsStreak) {
        this.resetsStreak = resetsStreak;
    }

    public String getMissionType() {
        return missionType;
    }

    public void setMissionType(String missionType) {
        this.missionType = missionType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public double getProgress() {
        if (activitiesToComplete == 0) return 0;
        return (double) activitiesCompleted / activitiesToComplete;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > deadline && !isCompleted;
    }

    public int getRemainingActivities() {
        return Math.max(0, activitiesToComplete - activitiesCompleted);
    }

    public static class Builder {
        private String missionId;
        private String title = "Recovery Mission";
        private String description;
        private int activitiesToComplete = 2;
        private int activitiesCompleted = 0;
        private long deadline;
        private boolean isCompleted = false;
        private String userId;
        private int xpReward = 50;
        private boolean resetsStreak = true;
        private String missionType = "pattern_break";
        private long createdAt;
        private long completedAt;

        public Builder setMissionId(String missionId) {
            this.missionId = missionId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setActivitiesToComplete(int activitiesToComplete) {
            this.activitiesToComplete = activitiesToComplete;
            return this;
        }

        public Builder setActivitiesCompleted(int activitiesCompleted) {
            this.activitiesCompleted = activitiesCompleted;
            return this;
        }

        public Builder setDeadline(long deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setCompleted(boolean completed) {
            isCompleted = completed;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setXpReward(int xpReward) {
            this.xpReward = xpReward;
            return this;
        }

        public Builder setResetsStreak(boolean resetsStreak) {
            this.resetsStreak = resetsStreak;
            return this;
        }

        public Builder setMissionType(String missionType) {
            this.missionType = missionType;
            return this;
        }

        public Builder setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setCompletedAt(long completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public RecoveryMission build() {
            RecoveryMission mission = new RecoveryMission(missionId, title, description, activitiesToComplete,
                    activitiesCompleted, deadline, isCompleted, userId);
            mission.setXpReward(xpReward);
            mission.setResetsStreak(resetsStreak);
            mission.setMissionType(missionType);
            mission.setCreatedAt(createdAt);
            mission.setCompletedAt(completedAt);
            return mission;
        }
    }
}
