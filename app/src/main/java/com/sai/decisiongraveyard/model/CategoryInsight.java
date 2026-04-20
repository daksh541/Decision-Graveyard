package com.sai.decisiongraveyard.model;

public class CategoryInsight {

    private final String category;
    private final int evaluatedCount;
    private final int regretCount;
    private final int regretRate;

    public CategoryInsight(String category, int evaluatedCount, int regretCount, int regretRate) {
        this.category = category;
        this.evaluatedCount = evaluatedCount;
        this.regretCount = regretCount;
        this.regretRate = regretRate;
    }

    public String getCategory() {
        return category;
    }

    public int getEvaluatedCount() {
        return evaluatedCount;
    }

    public int getRegretCount() {
        return regretCount;
    }

    public int getRegretRate() {
        return regretRate;
    }
}
