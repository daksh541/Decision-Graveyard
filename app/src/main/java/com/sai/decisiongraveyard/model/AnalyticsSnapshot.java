package com.sai.decisiongraveyard.model;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsSnapshot {

    private final int totalDecisions;
    private final int totalEvaluated;
    private final int goodCount;
    private final int badCount;
    private final int neutralCount;
    private final int readyForReviewCount;
    private final int upcomingCount;
    private final List<CategoryInsight> categoryInsights;
    private final List<String> generatedInsights;
    private final int intensityLevel;
    private final List<String> actionableInsights;
    private final List<PatternRepetition> patternRepetitions;
    private final int consequencePoints;
    private final String consequenceMessage;
    private final int totalActivities;
    private final int completedActivities;
    private final int missedActivities;
    private final List<String> activityInsights;
    private final AiInsightReport aiInsightReport;
    private final List<HeatmapCell> heatmapCells;
    private final List<Integer> weeklyQualityTrend;

    public AnalyticsSnapshot(int totalDecisions, int totalEvaluated, int goodCount, int badCount,
                             int neutralCount, int readyForReviewCount, int upcomingCount,
                             List<CategoryInsight> categoryInsights, List<String> generatedInsights) {
        this(totalDecisions, totalEvaluated, goodCount, badCount, neutralCount, readyForReviewCount,
                upcomingCount, categoryInsights, generatedInsights, 0, new ArrayList<>(), new ArrayList<>(), 0, "",
                0, 0, 0, new ArrayList<>(), AiInsightReport.empty(), new ArrayList<>(), new ArrayList<>());
    }

    public AnalyticsSnapshot(int totalDecisions, int totalEvaluated, int goodCount, int badCount,
                             int neutralCount, int readyForReviewCount, int upcomingCount,
                             List<CategoryInsight> categoryInsights, List<String> generatedInsights,
                             int intensityLevel, List<String> actionableInsights,
                             List<PatternRepetition> patternRepetitions) {
        this(totalDecisions, totalEvaluated, goodCount, badCount, neutralCount, readyForReviewCount,
                upcomingCount, categoryInsights, generatedInsights, intensityLevel, actionableInsights,
                patternRepetitions, 0, "", 0, 0, 0, new ArrayList<>(), AiInsightReport.empty(), new ArrayList<>(), new ArrayList<>());
    }

    public AnalyticsSnapshot(int totalDecisions, int totalEvaluated, int goodCount, int badCount,
                             int neutralCount, int readyForReviewCount, int upcomingCount,
                             List<CategoryInsight> categoryInsights, List<String> generatedInsights,
                             int intensityLevel, List<String> actionableInsights,
                             List<PatternRepetition> patternRepetitions, int consequencePoints,
                             String consequenceMessage) {
        this(totalDecisions, totalEvaluated, goodCount, badCount, neutralCount, readyForReviewCount,
                upcomingCount, categoryInsights, generatedInsights, intensityLevel, actionableInsights,
                patternRepetitions, consequencePoints, consequenceMessage, 0, 0, 0, new ArrayList<>(), AiInsightReport.empty(), new ArrayList<>(), new ArrayList<>());
    }

    public AnalyticsSnapshot(int totalDecisions, int totalEvaluated, int goodCount, int badCount,
                             int neutralCount, int readyForReviewCount, int upcomingCount,
                             List<CategoryInsight> categoryInsights, List<String> generatedInsights,
                             int intensityLevel, List<String> actionableInsights,
                             List<PatternRepetition> patternRepetitions, int consequencePoints,
                             String consequenceMessage, int totalActivities, int completedActivities,
                             int missedActivities, List<String> activityInsights,
                             AiInsightReport aiInsightReport, List<HeatmapCell> heatmapCells,
                             List<Integer> weeklyQualityTrend) {
        this.totalDecisions = totalDecisions;
        this.totalEvaluated = totalEvaluated;
        this.goodCount = goodCount;
        this.badCount = badCount;
        this.neutralCount = neutralCount;
        this.readyForReviewCount = readyForReviewCount;
        this.upcomingCount = upcomingCount;
        this.categoryInsights = categoryInsights;
        this.generatedInsights = generatedInsights;
        this.intensityLevel = intensityLevel;
        this.actionableInsights = actionableInsights;
        this.patternRepetitions = patternRepetitions;
        this.consequencePoints = consequencePoints;
        this.consequenceMessage = consequenceMessage;
        this.totalActivities = totalActivities;
        this.completedActivities = completedActivities;
        this.missedActivities = missedActivities;
        this.activityInsights = activityInsights;
        this.aiInsightReport = aiInsightReport == null ? AiInsightReport.empty() : aiInsightReport;
        this.heatmapCells = heatmapCells == null ? new ArrayList<>() : heatmapCells;
        this.weeklyQualityTrend = weeklyQualityTrend == null ? new ArrayList<>() : weeklyQualityTrend;
    }

    public int getTotalDecisions() {
        return totalDecisions;
    }

    public int getTotalEvaluated() {
        return totalEvaluated;
    }

    public int getGoodCount() {
        return goodCount;
    }

    public int getBadCount() {
        return badCount;
    }

    public int getNeutralCount() {
        return neutralCount;
    }

    public int getReadyForReviewCount() {
        return readyForReviewCount;
    }

    public int getUpcomingCount() {
        return upcomingCount;
    }

    public List<CategoryInsight> getCategoryInsights() {
        return categoryInsights;
    }

    public List<String> getGeneratedInsights() {
        return generatedInsights;
    }

    public int getIntensityLevel() {
        return intensityLevel;
    }

    public List<String> getActionableInsights() {
        return actionableInsights;
    }

    public List<PatternRepetition> getPatternRepetitions() {
        return patternRepetitions;
    }

    public int getConsequencePoints() {
        return consequencePoints;
    }

    public String getConsequenceMessage() {
        return consequenceMessage;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public int getCompletedActivities() {
        return completedActivities;
    }

    public int getMissedActivities() {
        return missedActivities;
    }

    public List<String> getActivityInsights() {
        return activityInsights;
    }

    public AiInsightReport getAiInsightReport() {
        return aiInsightReport;
    }

    public List<HeatmapCell> getHeatmapCells() {
        return heatmapCells;
    }

    public List<Integer> getWeeklyQualityTrend() {
        return weeklyQualityTrend;
    }

    public boolean isEmpty() {
        return totalEvaluated == 0;
    }

    public int getGoodPercent() {
        return totalEvaluated == 0 ? 0 : (int) Math.round(goodCount * 100.0 / totalEvaluated);
    }

    public int getBadPercent() {
        return totalEvaluated == 0 ? 0 : (int) Math.round(badCount * 100.0 / totalEvaluated);
    }

    public int getNeutralPercent() {
        return totalEvaluated == 0 ? 0 : (int) Math.round(neutralCount * 100.0 / totalEvaluated);
    }

    public int getQualityScore() {
        return totalEvaluated == 0 ? 0 : (int) Math.round(goodCount * 100.0 / totalEvaluated);
    }

    public String getIdentityLabel() {
        int score = getQualityScore();
        if (score >= 80) return "Level 4: Disciplined";
        if (score >= 60) return "Level 3: Controlled";
        if (score >= 40) return "Level 2: Aware";
        return "Level 1: Chaotic";
    }

    public int getProgressionLevel() {
        int score = getQualityScore();
        if (score >= 80) return 4;
        if (score >= 60) return 3;
        if (score >= 40) return 2;
        return 1;
    }

    public static class PatternRepetition {
        private final String category;
        private final String timePattern;
        private final int count;
        private final String message;

        public PatternRepetition(String category, String timePattern, int count, String message) {
            this.category = category;
            this.timePattern = timePattern;
            this.count = count;
            this.message = message;
        }

        public String getCategory() { return category; }
        public String getTimePattern() { return timePattern; }
        public int getCount() { return count; }
        public String getMessage() { return message; }
    }
}
