package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.sai.decisiongraveyard.BuildConfig;
import com.sai.decisiongraveyard.model.AiInsightReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LLMService {

    public static class BehavioralContext {
        public int totalEvaluated;
        public int goodCount;
        public int badCount;
        public int neutralCount;
        public int readyForReviewCount;
        public int upcomingCount;
        public int activityCompletionRate;
        public int completedActivities;
        public int missedActivities;
        public int disciplineStreak;
        public int badStreak;
        public int behaviorScore;
        public int riskScore;
        public int disciplineScore;
        public int lateNightRisk;
        public int worstCategoryRegretRate;
        public String worstCategory = "unknown";
        public String worstTimeWindow = "Unknown";
        public String dominantEmotion = "Mixed";
        public String activityCorrelationHint = "";
        public String recoveryMissionHint = "";
        public String weeklyMomentum = "";
        public List<String> patternHighlights = Collections.emptyList();
        public List<String> weeklyTrend = Collections.emptyList();

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("totalEvaluated", totalEvaluated);
            json.put("goodCount", goodCount);
            json.put("badCount", badCount);
            json.put("neutralCount", neutralCount);
            json.put("readyForReviewCount", readyForReviewCount);
            json.put("upcomingCount", upcomingCount);
            json.put("activityCompletionRate", activityCompletionRate);
            json.put("completedActivities", completedActivities);
            json.put("missedActivities", missedActivities);
            json.put("disciplineStreak", disciplineStreak);
            json.put("badStreak", badStreak);
            json.put("behaviorScore", behaviorScore);
            json.put("riskScore", riskScore);
            json.put("disciplineScore", disciplineScore);
            json.put("lateNightRisk", lateNightRisk);
            json.put("worstCategoryRegretRate", worstCategoryRegretRate);
            json.put("worstCategory", worstCategory);
            json.put("worstTimeWindow", worstTimeWindow);
            json.put("dominantEmotion", dominantEmotion);
            json.put("activityCorrelationHint", activityCorrelationHint);
            json.put("recoveryMissionHint", recoveryMissionHint);
            json.put("weeklyMomentum", weeklyMomentum);
            json.put("patternHighlights", new JSONArray(patternHighlights));
            json.put("weeklyTrend", new JSONArray(weeklyTrend));
            return json;
        }
    }

    private static final String TAG = "LLMService";
    private static final String MODEL_NAME = "gemini-1.5-flash";
    private static final String PREFS_NAME = "gemini_behavior_cache";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(8);
    private static final long MIN_REQUEST_GAP_MS = 3500L;
    private static final long REQUEST_TIMEOUT_SECONDS = 25L;

    private final SharedPreferences cachePreferences;
    private final ExecutorService executorService;
    private final GenerativeModelFutures model;
    private volatile boolean aiEnabled;
    private volatile long lastRequestTimestamp;

    public LLMService(Context context) {
        Context appContext = context.getApplicationContext();
        this.cachePreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();

        String apiKey = BuildConfig.GEMINI_API_KEY;
        this.aiEnabled = apiKey != null && !apiKey.trim().isEmpty();
        if (!aiEnabled) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to deterministic insights.");
            this.model = null;
            return;
        }

        GenerativeModel generativeModel = new GenerativeModel(MODEL_NAME, apiKey);
        this.model = GenerativeModelFutures.from(generativeModel);
    }

    public boolean isAiEnabled() {
        return aiEnabled && model != null;
    }

    public CompletableFuture<String> generateInsights(String decisionData, boolean brutalMode) {
        BehavioralContext context = new BehavioralContext();
        context.totalEvaluated = extractInt(decisionData, "Total evaluated decisions:");
        context.goodCount = extractInt(decisionData, "Good decisions:");
        context.badCount = extractInt(decisionData, "Bad decisions:");
        context.behaviorScore = clamp(100 - extractInt(decisionData, "Intensity level (bad decision rate):"));
        context.riskScore = extractInt(decisionData, "Intensity level (bad decision rate):");
        context.disciplineScore = context.behaviorScore;
        context.worstCategory = extractText(decisionData, "Worst category:");
        context.worstTimeWindow = extractText(decisionData, "Worst time:");

        return CompletableFuture.supplyAsync(() -> {
            AiInsightReport report = generateBehavioralReport(context, brutalMode);
            return joinLines(report.getInsightCards());
        }, executorService);
    }

    public CompletableFuture<String> generateActionableInsights(String decisionData) {
        BehavioralContext context = new BehavioralContext();
        context.totalEvaluated = extractInt(decisionData, "Total evaluated decisions:");
        context.goodCount = extractInt(decisionData, "Good decisions:");
        context.badCount = extractInt(decisionData, "Bad decisions:");
        context.behaviorScore = clamp((context.goodCount * 100) / Math.max(1, context.totalEvaluated));
        context.riskScore = clamp((context.badCount * 100) / Math.max(1, context.totalEvaluated));
        context.disciplineScore = context.behaviorScore;

        return CompletableFuture.supplyAsync(() -> {
            AiInsightReport report = generateBehavioralReport(context, false);
            return joinLines(report.getRecommendations());
        }, executorService);
    }

    public CompletableFuture<String> generateActivityInsights(String activityData) {
        BehavioralContext context = new BehavioralContext();
        context.activityCorrelationHint = activityData;

        return CompletableFuture.supplyAsync(() -> {
            AiInsightReport report = generateBehavioralReport(context, false);
            List<String> lines = new ArrayList<>();
            if (!report.getActivityCorrelation().isEmpty()) {
                lines.add(report.getActivityCorrelation());
            }
            if (!report.getRecoverySuggestion().isEmpty()) {
                lines.add(report.getRecoverySuggestion());
            }
            return joinLines(lines);
        }, executorService);
    }

    public AiInsightReport generateBehavioralReport(@NonNull BehavioralContext context, boolean brutalMode) {
        String cacheKey = buildCacheKey(context, brutalMode);
        AiInsightReport cached = readCachedReport(cacheKey);
        if (cached != null) {
            return cached.withCacheFlag(true);
        }

        if (!isAiEnabled()) {
            return buildFallbackReport(context, brutalMode, "Gemini unavailable");
        }

        long now = System.currentTimeMillis();
        if (now - lastRequestTimestamp < MIN_REQUEST_GAP_MS) {
            return buildFallbackReport(context, brutalMode, "Rate limited");
        }

        try {
            lastRequestTimestamp = now;
            String prompt = buildPrompt(context, brutalMode);
            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> future = model.generateContent(content);
            GenerateContentResponse response = future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String rawText = response == null ? null : response.getText();
            if (rawText == null || rawText.trim().isEmpty()) {
                return buildFallbackReport(context, brutalMode, "Empty Gemini response");
            }

            AiInsightReport report = parseResponse(rawText);
            cacheReport(cacheKey, report);
            return report;
        } catch (Exception exception) {
            disableIfInvalidApiKey(exception);
            Log.e(TAG, "Gemini insight generation failed", exception);
            return buildFallbackReport(context, brutalMode, exception.getMessage());
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private String buildPrompt(BehavioralContext context, boolean brutalMode) throws JSONException {
        String tone = brutalMode
                ? "brutally honest, sharp, emotionally precise, but still useful"
                : "calm, premium, precise, emotionally intelligent, and practical";

        return "You are an elite behavioral analyst inside an Android self-improvement app. "
                + "Turn the user's behavioral telemetry into an emotionally intelligent weekly AI report.\n"
                + "Tone: " + tone + ".\n"
                + "Never mention missing data, prompt wording, JSON, the model, or that you are an AI.\n"
                + "Use concrete language grounded in the telemetry.\n"
                + "Return only valid JSON with this exact schema:\n"
                + "{\n"
                + "  \"behaviorScore\": 0-100 integer,\n"
                + "  \"disciplineScore\": 0-100 integer,\n"
                + "  \"riskScore\": 0-100 integer,\n"
                + "  \"coachPersona\": \"short label\",\n"
                + "  \"summary\": \"2 short sentences max\",\n"
                + "  \"brainPattern\": \"1 sentence\",\n"
                + "  \"riskyTimeWindow\": \"1 sentence\",\n"
                + "  \"emotionalPattern\": \"1 sentence\",\n"
                + "  \"regretPattern\": \"1 sentence\",\n"
                + "  \"streakAnalysis\": \"1 sentence\",\n"
                + "  \"activityCorrelation\": \"1 sentence\",\n"
                + "  \"recoverySuggestion\": \"1 sentence\",\n"
                + "  \"weeklyReport\": \"3 short sentences max\",\n"
                + "  \"quote\": \"short punchy quote\",\n"
                + "  \"insightCards\": [\"3 concise strings\"],\n"
                + "  \"recommendations\": [\"3 concise strings\"],\n"
                + "  \"weeklyPriorities\": [\"3 concise strings\"]\n"
                + "}\n"
                + "Telemetry:\n"
                + context.toJson().toString(2);
    }

    private AiInsightReport parseResponse(String rawText) throws JSONException {
        String normalized = normalizeJsonPayload(rawText);
        JSONObject json = new JSONObject(normalized);
        AiInsightReport report = AiInsightReport.fromJson(json);
        return new AiInsightReport(
                clamp(report.getBehaviorScore()),
                clamp(report.getDisciplineScore()),
                clamp(report.getRiskScore()),
                emptyIfNeeded(report.getCoachPersona(), "Signal Coach"),
                report.getSummary(),
                report.getBrainPattern(),
                report.getRiskyTimeWindow(),
                report.getEmotionalPattern(),
                report.getRegretPattern(),
                report.getStreakAnalysis(),
                report.getActivityCorrelation(),
                report.getRecoverySuggestion(),
                report.getWeeklyReport(),
                emptyIfNeeded(report.getQuote(), "Small controlled choices compound."),
                ensureThree(report.getInsightCards(), "Signal is still forming."),
                ensureThree(report.getRecommendations(), "Keep logging decisions honestly."),
                ensureThree(report.getWeeklyPriorities(), "Protect your strongest routine."),
                true,
                false
        );
    }

    private AiInsightReport buildFallbackReport(BehavioralContext context, boolean brutalMode, String reason) {
        int behaviorScore = clamp(context.behaviorScore > 0
                ? context.behaviorScore
                : safePercent(context.goodCount, Math.max(1, context.totalEvaluated)));
        int riskScore = clamp(context.riskScore > 0
                ? context.riskScore
                : safePercent(context.badCount, Math.max(1, context.totalEvaluated)));
        int disciplineScore = clamp(context.disciplineScore > 0
                ? context.disciplineScore
                : (int) Math.round((behaviorScore * 0.6f) + (context.activityCompletionRate * 0.4f)));

        String coachPersona = brutalMode ? "Brutal Coach" : "Signal Coach";
        String summary = buildSummary(context, behaviorScore, riskScore, brutalMode);
        String riskyWindow = context.worstTimeWindow == null || context.worstTimeWindow.trim().isEmpty()
                ? "Your risky window is still forming."
                : context.worstTimeWindow + " is still the weakest part of your day.";
        String emotionalPattern = context.dominantEmotion == null || context.dominantEmotion.trim().isEmpty()
                ? "Your emotional signal is mixed, but hesitation rises when structure drops."
                : context.dominantEmotion + " is shaping the way you decide.";
        String regretPattern = context.worstCategoryRegretRate >= 50
                ? "Your regret clusters around " + context.worstCategory + " decisions."
                : "Your regret pattern is present, but not yet dominant in one category.";
        String streakAnalysis = context.badStreak >= 2
                ? "The negative streak is active. Interrupt it with one clean, easy win."
                : context.disciplineStreak >= 3
                ? "Your streak is carrying real momentum right now."
                : "Your streak signal is fragile. Daily consistency matters more than intensity.";
        String activityCorrelation = emptyIfNeeded(
                context.activityCorrelationHint,
                context.missedActivities > context.completedActivities
                        ? "Missed activities are likely reducing decision quality later in the day."
                        : "Completed activities are helping stabilize your later decisions."
        );
        String recoverySuggestion = emptyIfNeeded(
                context.recoveryMissionHint,
                context.badStreak >= 2
                        ? "Shrink the day. Complete one simple task before making any high-risk choice."
                        : "Protect the next 3 hours with structure before emotion takes over."
        );
        String weeklyReport = buildWeeklyReport(context, behaviorScore, riskScore);
        String quote = brutalMode
                ? "Discipline is what remains after excuses get cut."
                : "Tiny deliberate choices can change the shape of a week.";

        List<String> insightCards = new ArrayList<>();
        insightCards.add(context.totalEvaluated == 0
                ? "Start logging more decisions to sharpen the model."
                : "Behavior score is " + behaviorScore + " with risk pressure at " + riskScore + ".");
        insightCards.add(context.worstCategoryRegretRate >= 50
                ? "Your highest regret cluster is " + context.worstCategory + "."
                : "No category has fully taken over your regret pattern.");
        insightCards.add(context.lateNightRisk >= 50
                ? "Late-night behavior is still expensive for you."
                : "Time-of-day risk is present, but not dominating yet.");

        List<String> recommendations = new ArrayList<>();
        recommendations.add(context.lateNightRisk >= 50
                ? "Delay big decisions until morning when possible."
                : "Create a short pause before any emotionally loaded choice.");
        recommendations.add(context.missedActivities > context.completedActivities
                ? "Reduce the daily plan and finish the first commitment early."
                : "Protect your current routine and stack one more repeatable win.");
        recommendations.add(context.badStreak >= 2
                ? "Use brutal mode only after you complete one recovery action."
                : "Review one past decision tonight and name the real trigger.");

        List<String> weeklyPriorities = new ArrayList<>();
        weeklyPriorities.add(context.worstCategoryRegretRate >= 50
                ? "Control your " + context.worstCategory + " decisions."
                : "Protect consistency before chasing intensity.");
        weeklyPriorities.add(context.activityCompletionRate < 60
                ? "Lift your completion rate above 60%."
                : "Keep completion rate above today's baseline.");
        weeklyPriorities.add(context.readyForReviewCount > 0
                ? "Review overdue decisions and close the loop."
                : "Keep feedback loops tight with same-day reflection.");

        if (reason != null && !reason.trim().isEmpty()) {
            Log.w(TAG, "Using deterministic insight fallback: " + reason);
        }

        return new AiInsightReport(
                behaviorScore,
                disciplineScore,
                riskScore,
                coachPersona,
                summary,
                emptyIfNeeded(context.weeklyMomentum, "Your pattern is adapting to what you repeat."),
                riskyWindow,
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
                false,
                false
        );
    }

    private String buildSummary(BehavioralContext context, int behaviorScore, int riskScore, boolean brutalMode) {
        if (context.totalEvaluated == 0) {
            return brutalMode
                    ? "You have not built enough signal yet. Track honestly before asking for a verdict."
                    : "Your signal is still early. A few more reviewed decisions will sharpen the pattern.";
        }
        if (riskScore >= 70) {
            return brutalMode
                    ? "Your recent pattern is unstable. Emotion is steering faster than discipline."
                    : "Your recent pattern is unstable, and emotional choices are outpacing structure.";
        }
        if (behaviorScore >= 75) {
            return "You are showing real control. The key now is protecting consistency under pressure.";
        }
        return "You are in a transitional pattern. Better structure will raise your floor before motivation does.";
    }

    private String buildWeeklyReport(BehavioralContext context, int behaviorScore, int riskScore) {
        if (context.totalEvaluated == 0) {
            return "The week is still in setup mode. Start reviewing decisions and activities so the pattern can lock in.";
        }
        if (riskScore >= 70) {
            return "This week shows concentrated downside. Your risk rises when structure slips and decisions become reactive. Reduce optional choices during weak hours.";
        }
        if (behaviorScore >= 75) {
            return "This week looks stronger than your recent baseline. Your disciplined moments are beginning to cluster. The next gain comes from protecting evenings and keeping review loops short.";
        }
        return "This week is mixed but recoverable. You are not failing everywhere. You are leaking energy in a few repeated situations that can be tightened quickly.";
    }

    private void cacheReport(String key, AiInsightReport report) {
        try {
            cachePreferences.edit()
                    .putString(key + "_json", report.toJson().toString())
                    .putLong(key + "_timestamp", System.currentTimeMillis())
                    .apply();
        } catch (JSONException exception) {
            Log.w(TAG, "Failed to cache Gemini report", exception);
        }
    }

    private AiInsightReport readCachedReport(String key) {
        long timestamp = cachePreferences.getLong(key + "_timestamp", 0L);
        if (timestamp <= 0L || System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
            return null;
        }
        String json = cachePreferences.getString(key + "_json", null);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return AiInsightReport.fromJson(new JSONObject(json));
        } catch (JSONException exception) {
            Log.w(TAG, "Cached Gemini report was invalid", exception);
            return null;
        }
    }

    private String buildCacheKey(BehavioralContext context, boolean brutalMode) {
        try {
            return sha1((brutalMode ? "brutal" : "balanced") + "|" + context.toJson().toString());
        } catch (Exception exception) {
            return String.valueOf((brutalMode ? "brutal" : "balanced").hashCode());
        }
    }

    private String normalizeJsonPayload(String rawText) {
        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceAll("```$", "")
                    .trim();
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private void disableIfInvalidApiKey(Exception exception) {
        if (exception == null || !aiEnabled) {
            return;
        }
        String message = exception.getMessage();
        if (message != null && message.toLowerCase(Locale.US).contains("api key")) {
            aiEnabled = false;
            Log.w(TAG, "Disabling Gemini for this session because the API key is invalid.");
        }
    }

    private int extractInt(String source, String prefix) {
        if (source == null || prefix == null) {
            return 0;
        }
        for (String line : source.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String digits = trimmed.substring(prefix.length()).replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private String extractText(String source, String prefix) {
        if (source == null || prefix == null) {
            return "";
        }
        for (String line : source.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                int parenIndex = value.indexOf('(');
                return parenIndex > 0 ? value.substring(0, parenIndex).trim() : value;
            }
        }
        return "";
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.trim());
        }
        return builder.toString();
    }

    private String emptyIfNeeded(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private List<String> ensureThree(List<String> input, String fallback) {
        List<String> output = new ArrayList<>();
        if (input != null) {
            for (String value : input) {
                if (value != null && !value.trim().isEmpty()) {
                    output.add(value.trim());
                }
                if (output.size() == 3) {
                    return output;
                }
            }
        }
        while (output.size() < 3) {
            output.add(fallback);
        }
        return output;
    }

    private int safePercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return clamp((int) Math.round((numerator * 100.0) / denominator));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String sha1(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value));
        }
        return builder.toString();
    }
}
