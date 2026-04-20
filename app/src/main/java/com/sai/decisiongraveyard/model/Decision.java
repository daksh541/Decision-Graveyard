package com.sai.decisiongraveyard.model;

public class Decision {

    private long id;
    private String title;
    private String description;
    private String category;
    private long decisionTime;
    private long evaluationTime;
    private int decisionHour;
    private String userId;
    private String outcome;
    private String reflectionNotes;
    private Long evaluatedAt;

    public Decision() {
    }

    public Decision(String title, String description, String category,
                    long decisionTime, long evaluationTime, int decisionHour) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.decisionTime = decisionTime;
        this.evaluationTime = evaluationTime;
        this.decisionHour = decisionHour;
    }

    public Decision(String title, String description, String category,
                    long decisionTime, long evaluationTime, int decisionHour, String userId) {
        this(title, description, category, decisionTime, evaluationTime, decisionHour);
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDecisionTime() {
        return decisionTime;
    }

    public void setDecisionTime(long decisionTime) {
        this.decisionTime = decisionTime;
    }

    public long getCreatedAt() {
        return decisionTime;
    }

    public void setCreatedAt(long createdAt) {
        this.decisionTime = createdAt;
    }

    public long getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(long evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public int getDecisionHour() {
        return decisionHour;
    }

    public void setDecisionHour(int decisionHour) {
        this.decisionHour = decisionHour;
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

    public String getReflectionNotes() {
        return reflectionNotes;
    }

    public void setReflectionNotes(String reflectionNotes) {
        this.reflectionNotes = reflectionNotes;
    }

    public Long getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Long evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", userId='" + userId + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
