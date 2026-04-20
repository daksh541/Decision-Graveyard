package com.sai.decisiongraveyard.model;

public class TimelineEvent {

    private String eventId;
    private String eventType;
    private String title;
    private String category;
    private String status;
    private long timestamp;
    private String userId;
    private String outcome;
    private String description;

    public TimelineEvent() {
    }

    public TimelineEvent(String eventType, String title, String category, 
                         String status, long timestamp, String userId) {
        this.eventType = eventType;
        this.title = title;
        this.category = category;
        this.status = status;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public static TimelineEvent fromDecision(Decision decision) {
        TimelineEvent event = new TimelineEvent();
        event.setEventType("decision");
        event.setTitle(decision.getTitle());
        event.setCategory(decision.getCategory());
        event.setTimestamp(decision.getDecisionTime());
        event.setUserId(decision.getUserId());
        event.setDescription(decision.getDescription());
        
        if (decision.getOutcome() != null) {
            event.setOutcome(decision.getOutcome());
            event.setStatus(decision.getOutcome().toLowerCase());
        } else {
            event.setStatus("pending");
        }
        
        return event;
    }

    public static TimelineEvent fromActivity(Activity activity) {
        TimelineEvent event = new TimelineEvent();
        event.setEventType("activity");
        event.setTitle(activity.getTitle());
        event.setCategory(activity.getCategory());
        event.setTimestamp(activity.getScheduledTime());
        event.setUserId(activity.getUserId());
        event.setStatus(activity.getStatus());
        
        return event;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPositive() {
        return "good".equals(status) || "completed".equals(status);
    }

    public boolean isNegative() {
        return "bad".equals(status) || "missed".equals(status);
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    @Override
    public String toString() {
        return "TimelineEvent{" +
                "eventType='" + eventType + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
