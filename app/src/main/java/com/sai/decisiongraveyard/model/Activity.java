package com.sai.decisiongraveyard.model;

public class Activity {

    private long activityId;
    private String title;
    private String category;
    private long scheduledTime;
    private boolean completed;
    private Long completedTime;
    private String userId;
    private long createdAt;
    private String status;

    public Activity() {
    }

    public Activity(String title, String category, long scheduledTime, String userId) {
        this.title = title;
        this.category = category;
        this.scheduledTime = scheduledTime;
        this.userId = userId;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Long completedTime) {
        this.completedTime = completedTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        if (completed) {
            return "completed";
        }
        if (System.currentTimeMillis() > scheduledTime) {
            return "missed";
        }
        return "pending";
    }

    @Override
    public String toString() {
        return "Activity{" +
                "activityId=" + activityId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
