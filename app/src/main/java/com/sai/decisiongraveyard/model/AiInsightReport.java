package com.sai.decisiongraveyard.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiInsightReport {

    private final int behaviorScore;
    private final int disciplineScore;
    private final int riskScore;
    private final String coachPersona;
    private final String summary;
    private final String brainPattern;
    private final String riskyTimeWindow;
    private final String emotionalPattern;
    private final String regretPattern;
    private final String streakAnalysis;
    private final String activityCorrelation;
    private final String recoverySuggestion;
    private final String weeklyReport;
    private final String quote;
    private final List<String> insightCards;
    private final List<String> recommendations;
    private final List<String> weeklyPriorities;
    private final boolean aiGenerated;
    private final boolean fromCache;

    public AiInsightReport(
            int behaviorScore,
            int disciplineScore,
            int riskScore,
            String coachPersona,
            String summary,
            String brainPattern,
            String riskyTimeWindow,
            String emotionalPattern,
            String regretPattern,
            String streakAnalysis,
            String activityCorrelation,
            String recoverySuggestion,
            String weeklyReport,
            String quote,
            List<String> insightCards,
            List<String> recommendations,
            List<String> weeklyPriorities,
            boolean aiGenerated,
            boolean fromCache
    ) {
        this.behaviorScore = behaviorScore;
        this.disciplineScore = disciplineScore;
        this.riskScore = riskScore;
        this.coachPersona = safe(coachPersona);
        this.summary = safe(summary);
        this.brainPattern = safe(brainPattern);
        this.riskyTimeWindow = safe(riskyTimeWindow);
        this.emotionalPattern = safe(emotionalPattern);
        this.regretPattern = safe(regretPattern);
        this.streakAnalysis = safe(streakAnalysis);
        this.activityCorrelation = safe(activityCorrelation);
        this.recoverySuggestion = safe(recoverySuggestion);
        this.weeklyReport = safe(weeklyReport);
        this.quote = safe(quote);
        this.insightCards = immutableList(insightCards);
        this.recommendations = immutableList(recommendations);
        this.weeklyPriorities = immutableList(weeklyPriorities);
        this.aiGenerated = aiGenerated;
        this.fromCache = fromCache;
    }

    public static AiInsightReport empty() {
        return new AiInsightReport(
                0,
                0,
                0,
                "Signal Coach",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                false
        );
    }

    public AiInsightReport withCacheFlag(boolean cached) {
        return new AiInsightReport(
                behaviorScore,
                disciplineScore,
                riskScore,
                coachPersona,
                summary,
                brainPattern,
                riskyTimeWindow,
                emotionalPattern,
                regretPattern,
                streakAnalysis,
                activityCorrelation,
                recoverySuggestion,
                weeklyReport,
                quote,
                insightCards,
                recommendations,
                weeklyPriorities,
                aiGenerated,
                cached
        );
    }

    public int getBehaviorScore() {
        return behaviorScore;
    }

    public int getDisciplineScore() {
        return disciplineScore;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getCoachPersona() {
        return coachPersona;
    }

    public String getSummary() {
        return summary;
    }

    public String getBrainPattern() {
        return brainPattern;
    }

    public String getRiskyTimeWindow() {
        return riskyTimeWindow;
    }

    public String getEmotionalPattern() {
        return emotionalPattern;
    }

    public String getRegretPattern() {
        return regretPattern;
    }

    public String getStreakAnalysis() {
        return streakAnalysis;
    }

    public String getActivityCorrelation() {
        return activityCorrelation;
    }

    public String getRecoverySuggestion() {
        return recoverySuggestion;
    }

    public String getWeeklyReport() {
        return weeklyReport;
    }

    public String getQuote() {
        return quote;
    }

    public List<String> getInsightCards() {
        return insightCards;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public List<String> getWeeklyPriorities() {
        return weeklyPriorities;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("behaviorScore", behaviorScore);
        json.put("disciplineScore", disciplineScore);
        json.put("riskScore", riskScore);
        json.put("coachPersona", coachPersona);
        json.put("summary", summary);
        json.put("brainPattern", brainPattern);
        json.put("riskyTimeWindow", riskyTimeWindow);
        json.put("emotionalPattern", emotionalPattern);
        json.put("regretPattern", regretPattern);
        json.put("streakAnalysis", streakAnalysis);
        json.put("activityCorrelation", activityCorrelation);
        json.put("recoverySuggestion", recoverySuggestion);
        json.put("weeklyReport", weeklyReport);
        json.put("quote", quote);
        json.put("insightCards", new JSONArray(insightCards));
        json.put("recommendations", new JSONArray(recommendations));
        json.put("weeklyPriorities", new JSONArray(weeklyPriorities));
        json.put("aiGenerated", aiGenerated);
        json.put("fromCache", fromCache);
        return json;
    }

    public static AiInsightReport fromJson(JSONObject json) {
        if (json == null) {
            return empty();
        }
        return new AiInsightReport(
                clamp(json.optInt("behaviorScore", 0)),
                clamp(json.optInt("disciplineScore", 0)),
                clamp(json.optInt("riskScore", 0)),
                json.optString("coachPersona", "Signal Coach"),
                json.optString("summary", ""),
                json.optString("brainPattern", ""),
                json.optString("riskyTimeWindow", ""),
                json.optString("emotionalPattern", ""),
                json.optString("regretPattern", ""),
                json.optString("streakAnalysis", ""),
                json.optString("activityCorrelation", ""),
                json.optString("recoverySuggestion", ""),
                json.optString("weeklyReport", ""),
                json.optString("quote", ""),
                toList(json.optJSONArray("insightCards")),
                toList(json.optJSONArray("recommendations")),
                toList(json.optJSONArray("weeklyPriorities")),
                json.optBoolean("aiGenerated", false),
                json.optBoolean("fromCache", false)
        );
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> immutableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> clean = new ArrayList<>();
        for (String value : values) {
            String trimmed = safe(value);
            if (!trimmed.isEmpty()) {
                clean.add(trimmed);
            }
        }
        return Collections.unmodifiableList(clean);
    }

    private static List<String> toList(JSONArray array) {
        if (array == null || array.length() == 0) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String value = safe(array.optString(i));
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
}
