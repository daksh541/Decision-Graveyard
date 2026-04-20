package com.sai.decisiongraveyard.model;

import java.util.List;

public class CauseEffectInsight {

    private String insightId;
    private String userId;
    private String category;
    private String correlationType;
    private String insightMessage;
    private int missedActivityCount;
    private int badDecisionCount;
    private List<String> affectedCategories;
    private long generatedAt;
    private long startDate;
    private long endDate;

    public CauseEffectInsight() {
    }

    public CauseEffectInsight(String userId, String category, String correlationType, 
                              String insightMessage, int missedActivityCount, int badDecisionCount) {
        this.userId = userId;
        this.category = category;
        this.correlationType = correlationType;
        this.insightMessage = insightMessage;
        this.missedActivityCount = missedActivityCount;
        this.badDecisionCount = badDecisionCount;
        this.generatedAt = System.currentTimeMillis();
    }

    public String getInsightId() {
        return insightId;
    }

    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCorrelationType() {
        return correlationType;
    }

    public void setCorrelationType(String correlationType) {
        this.correlationType = correlationType;
    }

    public String getInsightMessage() {
        return insightMessage;
    }

    public void setInsightMessage(String insightMessage) {
        this.insightMessage = insightMessage;
    }

    public int getMissedActivityCount() {
        return missedActivityCount;
    }

    public void setMissedActivityCount(int missedActivityCount) {
        this.missedActivityCount = missedActivityCount;
    }

    public int getBadDecisionCount() {
        return badDecisionCount;
    }

    public void setBadDecisionCount(int badDecisionCount) {
        this.badDecisionCount = badDecisionCount;
    }

    public List<String> getAffectedCategories() {
        return affectedCategories;
    }

    public void setAffectedCategories(List<String> affectedCategories) {
        this.affectedCategories = affectedCategories;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "CauseEffectInsight{" +
                "category='" + category + '\'' +
                ", correlationType='" + correlationType + '\'' +
                ", insightMessage='" + insightMessage + '\'' +
                ", missedActivityCount=" + missedActivityCount +
                ", badDecisionCount=" + badDecisionCount +
                '}';
    }
}
