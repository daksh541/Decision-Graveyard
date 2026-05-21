package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.AiInsightReport;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot.PatternRepetition;
import com.sai.decisiongraveyard.model.CategoryInsight;
import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.model.Evaluation;
import com.sai.decisiongraveyard.model.HeatmapCell;
import com.sai.decisiongraveyard.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class DecisionRepository {

    public interface DecisionRecordsListener {
        void onChanged(@NonNull List<DecisionRecord> records);

        void onError(@NonNull String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    private static final String TAG = "DecisionRepository";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final MutableLiveData<Long> dataChangedTrigger = new MutableLiveData<>();
    private ActivityRepository activityRepository;
    private RecoveryMissionRepository recoveryMissionRepository;
    private UserProfileRepository userProfileRepository;
    private LLMService llmService;
    private CauseEffectRepository causeEffectRepository;

    public DecisionRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        // RepositoryProvider will handle injection
    }

    public void setActivityRepository(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void setRecoveryMissionRepository(RecoveryMissionRepository recoveryMissionRepository) {
        this.recoveryMissionRepository = recoveryMissionRepository;
    }

    public void setUserProfileRepository(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public void setLLMService(LLMService llmService) {
        this.llmService = llmService;
    }

    public void setCauseEffectRepository(CauseEffectRepository causeEffectRepository) {
        this.causeEffectRepository = causeEffectRepository;
    }

    public LiveData<Long> getDataChangedTrigger() {
        return dataChangedTrigger;
    }

    public void createDecision(long decisionId, String title, String description, String category, long evaluationTime, ActionCallback callback) {
        try {
            Log.d(TAG, "createDecision (async) called with title: " + title + ", decisionId: " + decisionId);
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Log.e(TAG, "Save failed: No authenticated user.");
                callback.onError("Please log in to continue.");
                return;
            }
            String userId = user.getUid();
            Log.d(TAG, "User authenticated with userId: " + userId);

            long now = System.currentTimeMillis();
            Log.d(TAG, "Using provided decisionId: " + decisionId);

            Decision decision = new Decision(
                    title,
                    description,
                    category,
                    now,
                    evaluationTime,
                    DateUtils.getHourOfDay(now),
                    userId
            );
            decision.setId(decisionId);
            decision.setReflectionNotes("");

            Log.d(TAG, "Saving decision: title=" + title + ", userId=" + userId + ", object=" + decision);

            decisionDocument(decisionId).set(decision)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Decision saved successfully with ID: " + decisionId);
                        dataChangedTrigger.postValue(decisionId);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save decision: " + e.getMessage(), e);
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save decision");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in createDecision: " + e.getMessage(), e);
            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save decision");
        }
    }

    public long createDecision(String title, String description, String category, long evaluationTime) {
        try {
            Log.d(TAG, "createDecision (blocking) called with title: " + title);
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Log.e(TAG, "Save failed: No authenticated user.");
                throw new IllegalStateException("Please log in to continue.");
            }
            String userId = user.getUid();
            Log.d(TAG, "User authenticated with userId: " + userId);

            long now = System.currentTimeMillis();
            long decisionId = ThreadLocalRandom.current().nextLong(1_000_000_000_000L, Long.MAX_VALUE);
            Log.d(TAG, "Generated decisionId: " + decisionId);

            Decision decision = new Decision(
                    title,
                    description,
                    category,
                    now,
                    evaluationTime,
                    DateUtils.getHourOfDay(now),
                    userId
            );
            decision.setId(decisionId);
            decision.setReflectionNotes("");

            Log.d(TAG, "Saving decision: title=" + title + ", userId=" + userId + ", object=" + decision);

            await(decisionDocument(decisionId).set(decision));
            Log.d(TAG, "Decision saved successfully with ID: " + decisionId);
            return decisionId;
        } catch (Exception e) {
            Log.e(TAG, "Exception in createDecision: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean hasSimilarDecision(String title, String category, long referenceTime) {
        long comparisonStart = referenceTime - 24L * 60L * 60L * 1000L;
        for (DecisionRecord record : getAllDecisionRecords()) {
            Decision decision = record.getDecision();
            if (decision.getDecisionTime() < comparisonStart) {
                continue;
            }
            if (decision.getTitle() != null
                    && decision.getTitle().trim().equalsIgnoreCase(title)
                    && decision.getCategory() != null
                    && decision.getCategory().equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public DecisionWarning checkDecisionWarning(String category, int decisionHour) {
        List<DecisionRecord> records = getAllDecisionRecords();
        int totalInPattern = 0;
        int badInPattern = 0;

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) continue;

            Decision decision = record.getDecision();
            boolean categoryMatch = decision.getCategory() != null &&
                    decision.getCategory().equalsIgnoreCase(category);
            boolean timeMatch = Math.abs(decision.getDecisionHour() - decisionHour) <= 1;

            if (categoryMatch && timeMatch) {
                totalInPattern++;
                if ("bad".equals(record.getEvaluation().getOutcome())) {
                    badInPattern++;
                }
            }
        }

        if (totalInPattern < 3) return null;

        double regretRate = (double) badInPattern / totalInPattern;
        if (regretRate >= 0.6) {
            List<String> recentOutcomes = getRecentSimilarDecisionOutcomes(category, decisionHour, 3);
            String message;
            if (!recentOutcomes.isEmpty()) {
                message = "Last " + recentOutcomes.size() + " similar decisions: " + String.join(", ", recentOutcomes);
            } else {
                message = "Last time you made this type of decision, it went BAD.";
            }
            return new DecisionWarning(
                    true,
                    message,
                    (int) (regretRate * 100),
                    recentOutcomes
            );
        }
        return null;
    }

    public static class DecisionWarning {
        public final boolean shouldWarn;
        public final String message;
        public final int regretRate;
        public final List<String> recentOutcomes;

        public DecisionWarning(boolean shouldWarn, String message, int regretRate, List<String> recentOutcomes) {
            this.shouldWarn = shouldWarn;
            this.message = message;
            this.regretRate = regretRate;
            this.recentOutcomes = recentOutcomes;
        }
    }

    public List<String> getRecentSimilarDecisionOutcomes(String category, int decisionHour, int limit) {
        List<DecisionRecord> records = getAllDecisionRecords();
        List<String> outcomes = new ArrayList<>();

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) continue;

            Decision decision = record.getDecision();
            boolean categoryMatch = decision.getCategory() != null &&
                    decision.getCategory().equalsIgnoreCase(category);
            boolean timeMatch = Math.abs(decision.getDecisionHour() - decisionHour) <= 1;

            if (categoryMatch && timeMatch) {
                String outcome = record.getEvaluation().getOutcome();
                if ("good".equals(outcome)) {
                    outcomes.add("GOOD");
                } else if ("bad".equals(outcome)) {
                    outcomes.add("BAD");
                } else {
                    outcomes.add("NEUTRAL");
                }

                if (outcomes.size() >= limit) {
                    break;
                }
            }
        }

        return outcomes;
    }

    public List<DecisionRecord> getAllDecisionRecords() {
        String userId = requireUserId();
        Log.d(TAG, "Fetching all decisions for userId: " + userId);

        QuerySnapshot snapshot = await(
                firestore.collection("decisions")
                        .whereEqualTo("userId", userId)
                        .get()
        );

        Log.d(TAG, "Fetched " + snapshot.size() + " documents");
        for (QueryDocumentSnapshot doc : snapshot) {
            Log.d(TAG, "Document ID: " + doc.getId() + ", userId: " + doc.get("userId"));
        }

        return mapDecisionRecords(snapshot);
    }

    public DecisionRecord getDecisionRecord(long decisionId) {
        DocumentSnapshot snapshot = await(decisionDocument(decisionId).get());
        if (!snapshot.exists()) {
            Log.w(TAG, "Decision document not found: " + decisionId);
            return null;
        }

        Decision decision = snapshot.toObject(Decision.class);
        if (decision == null) {
            Log.w(TAG, "Failed to deserialize decision: " + decisionId);
            return null;
        }
        
        String currentUserId = requireUserId();
        String decisionUserId = decision.getUserId();
        
        // Edge case: Handle null userId in decision
        if (decisionUserId == null) {
            Log.w(TAG, "Decision has null userId: " + decisionId);
            return null;
        }
        
        if (!currentUserId.equals(decisionUserId)) {
            Log.w(TAG, "Decision belongs to different user: " + decisionId);
            return null;
        }
        
        decision.setId(decisionId);
        return buildDecisionRecord(decision);
    }

    public void saveEvaluation(long decisionId, String outcome, String reflectionNotes) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("outcome", outcome);
        updates.put("reflectionNotes", reflectionNotes == null ? "" : reflectionNotes.trim());
        updates.put("evaluatedAt", System.currentTimeMillis());
        await(decisionDocument(decisionId).update(updates));
    }

    public void saveEvaluation(long decisionId, String outcome, String reflectionNotes, ActionCallback callback) {
        Map<String, Object> updates = new java.util.LinkedHashMap<>();
        updates.put("outcome", outcome);
        updates.put("reflectionNotes", reflectionNotes);
        updates.put("evaluatedAt", System.currentTimeMillis());

        decisionDocument(decisionId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    dataChangedTrigger.postValue(decisionId);
                    
                    // Award XP based on outcome
                    if (userProfileRepository != null) {
                        if ("good".equals(outcome)) {
                            userProfileRepository.addGoodDecision(new UserProfileRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onSuccess(); // Still succeed even if XP fails
                                }
                            });
                        } else if ("bad".equals(outcome)) {
                            userProfileRepository.addBadDecision(new UserProfileRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onSuccess(); // Still succeed even if XP fails
                                }
                            });
                        } else {
                            callback.onSuccess();
                        }
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save evaluation");
                });
    }

    public void deleteDecision(long decisionId) {
        await(decisionDocument(decisionId).delete());
    }

    public List<Decision> getPendingDecisions() {
        List<Decision> pendingDecisions = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (DecisionRecord record : getAllDecisionRecords()) {
            if (!record.isEvaluated() && record.getDecision().getEvaluationTime() > now) {
                pendingDecisions.add(record.getDecision());
            }
        }
        return pendingDecisions;
    }

    public ListenerRegistration listenToDecisions(@NonNull DecisionRecordsListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Listen failed: No authenticated user.");
            listener.onError("Please log in to see your decisions.");
            return null;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Setting up snapshot listener for userId: " + userId);

        return firestore.collection("decisions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error: " + error.getMessage() + ", code: " + error.getCode());
                        // Don't clear the list on error - just notify the listener
                        listener.onError(error.getMessage() == null
                                ? "Couldn't load decisions right now."
                                : error.getMessage());
                        return;
                    }
                    if (snapshots == null) {
                        Log.w(TAG, "Snapshot is null - returning empty list");
                        listener.onChanged(new ArrayList<>());
                        return;
                    }

                    Log.d(TAG, "Snapshot update: " + snapshots.size() + " documents, metadata: " +
                          (snapshots.getMetadata().hasPendingWrites() ? "pending writes" : "from server"));

                    List<DecisionRecord> records = mapDecisionRecords(snapshots);
                    Log.d(TAG, "Mapped " + records.size() + " decision records after filtering");
                    listener.onChanged(records);
                });
    }

    public AnalyticsSnapshot getAnalyticsSnapshot() {
        return getAnalyticsSnapshot(false);
    }

    public AnalyticsSnapshot getAnalyticsSnapshot(boolean brutalMode) {
        List<DecisionRecord> records = getAllDecisionRecords();
        int goodCount = 0;
        int badCount = 0;
        int neutralCount = 0;
        int readyCount = 0;
        int upcomingCount = 0;

        Map<String, int[]> categoryMap = new LinkedHashMap<>();
        categoryMap.put("money", new int[2]);
        categoryMap.put("health", new int[2]);
        categoryMap.put("study", new int[2]);
        categoryMap.put("personal", new int[2]);

        Map<String, int[]> timeBucketMap = new LinkedHashMap<>();
        timeBucketMap.put("Late night", new int[2]);
        timeBucketMap.put("Morning", new int[2]);
        timeBucketMap.put("Afternoon", new int[2]);
        timeBucketMap.put("Evening", new int[2]);

        for (DecisionRecord record : records) {
            if (record.isReadyForReview()) {
                readyCount++;
            } else if (record.isUpcoming()) {
                upcomingCount++;
            }

            if (!record.isEvaluated()) {
                continue;
            }

            String outcome = record.getEvaluation().getOutcome();
            if ("good".equals(outcome)) {
                goodCount++;
            } else if ("bad".equals(outcome)) {
                badCount++;
            } else {
                neutralCount++;
            }

            String category = record.getDecision().getCategory();
            int[] categoryStats = categoryMap.get(category);
            if (categoryStats != null) {
                categoryStats[0]++;
                if ("bad".equals(outcome)) {
                    categoryStats[1]++;
                }
            }

            String bucket = DateUtils.getHourLabel(record.getDecision().getDecisionHour());
            int[] bucketStats = timeBucketMap.get(bucket);
            if (bucketStats != null) {
                bucketStats[0]++;
                if ("bad".equals(outcome)) {
                    bucketStats[1]++;
                }
            }
        }

        int totalEvaluated = goodCount + badCount + neutralCount;
        List<CategoryInsight> categoryInsights = new ArrayList<>();
        CategoryInsight worstCategory = null;
        for (Map.Entry<String, int[]> entry : categoryMap.entrySet()) {
            int evaluatedCount = entry.getValue()[0];
            int regretCount = entry.getValue()[1];
            int regretRate = evaluatedCount == 0
                    ? 0
                    : (int) Math.round(regretCount * 100.0 / evaluatedCount);
            CategoryInsight insight = new CategoryInsight(
                    entry.getKey(),
                    evaluatedCount,
                    regretCount,
                    regretRate
            );
            categoryInsights.add(insight);

            if (evaluatedCount > 0 && (worstCategory == null || regretRate > worstCategory.getRegretRate())) {
                worstCategory = insight;
            }
        }

        String worstTimeLabel = null;
        int highestTimeRegretRate = -1;
        for (Map.Entry<String, int[]> entry : timeBucketMap.entrySet()) {
            int evaluatedCount = entry.getValue()[0];
            if (evaluatedCount == 0) {
                continue;
            }
            int regretRate = (int) Math.round(entry.getValue()[1] * 100.0 / evaluatedCount);
            if (regretRate > highestTimeRegretRate) {
                highestTimeRegretRate = regretRate;
                worstTimeLabel = entry.getKey();
            }
        }

        int intensityLevel = totalEvaluated == 0 ? 0 : (int) Math.round(badCount * 100.0 / totalEvaluated);
        int consequencePoints = (goodCount * 5) - (badCount * 10);
        String consequenceMessage = generateConsequenceMessage(consequencePoints);
        List<String> generatedInsights = generateInsights(
                worstCategory, worstTimeLabel, highestTimeRegretRate, readyCount,
                goodCount, badCount, totalEvaluated, brutalMode, intensityLevel
        );
        List<String> actionableInsights = generateActionableInsights(
                worstCategory, worstTimeLabel, highestTimeRegretRate, intensityLevel
        );
        List<PatternRepetition> patternRepetitions = detectPatternRepetitions(records);
        int totalActivities = 0;
        int completedActivities = 0;
        int missedActivities = 0;
        Map<String, Integer> missedByCategory = new LinkedHashMap<>();

        try {
            List<com.sai.decisiongraveyard.model.Activity> activities = activityRepository.getAllActivities();
            totalActivities = activities.size();
            
            for (com.sai.decisiongraveyard.model.Activity activity : activities) {
                String status = activity.getStatus();
                if ("completed".equals(status)) {
                    completedActivities++;
                } else if ("missed".equals(status)) {
                    missedActivities++;
                    String category = activity.getCategory();
                    missedByCategory.put(category, missedByCategory.getOrDefault(category, 0) + 1);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not fetch activity metrics: " + e.getMessage());
        }

        List<String> activityInsights = generateActivityInsights(missedByCategory, missedActivities, totalActivities);
        List<String> causeEffectInsights = generateCauseEffectInsights(records, missedByCategory, missedActivities);
        activityInsights.addAll(causeEffectInsights);
        String patternMessage = "";
        try {
            com.sai.decisiongraveyard.model.PatternTracker patternTracker = activityRepository.detectFailurePattern();
            if (patternTracker.isPatternDetected()) {
                patternMessage = patternTracker.getPatternMessage();
                activityInsights.add(patternMessage);
                if (recoveryMissionRepository.getActiveMission() == null) {
                    recoveryMissionRepository.createRecoveryMission(
                            patternTracker.getRecoveryActivitiesNeeded(),
                            new RecoveryMissionRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Recovery mission created");
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Failed to create recovery mission: " + error);
                                }
                            }
                    );
                    activityInsights.add("Complete " + patternTracker.getRecoveryActivitiesNeeded() + 
                            " activities today to regain control.");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not detect failure pattern: " + e.getMessage());
        }

        activityInsights = prioritizeInsights(activityInsights, patternMessage);
        List<Integer> weeklyQualityTrend = buildWeeklyTrendPoints(records);
        List<HeatmapCell> heatmapCells = buildHeatmapCells(records);
        int goodStreak = calculateCurrentGoodStreak(records);
        int badStreak = calculateCurrentBadStreak(records);
        int activityCompletionRate = totalActivities == 0
                ? 0
                : (int) Math.round((completedActivities * 100.0) / totalActivities);
        int behaviorScore = computeBehaviorScore(goodCount, totalEvaluated, activityCompletionRate, goodStreak, badStreak);
        int riskScore = computeRiskScore(badCount, totalEvaluated, highestTimeRegretRate, missedActivities, totalActivities);
        int disciplineScore = computeDisciplineScore(behaviorScore, activityCompletionRate, goodStreak);
        LLMService.BehavioralContext behavioralContext = buildBehavioralContext(
                totalEvaluated,
                goodCount,
                badCount,
                neutralCount,
                readyCount,
                upcomingCount,
                activityCompletionRate,
                completedActivities,
                missedActivities,
                goodStreak,
                badStreak,
                behaviorScore,
                riskScore,
                disciplineScore,
                highestTimeRegretRate,
                worstCategory,
                worstTimeLabel,
                patternRepetitions,
                activityInsights,
                weeklyQualityTrend
        );
        AiInsightReport aiInsightReport = llmService == null
                ? AiInsightReport.empty()
                : llmService.generateBehavioralReport(behavioralContext, brutalMode);

        return new AnalyticsSnapshot(
                records.size(),
                totalEvaluated,
                goodCount,
                badCount,
                neutralCount,
                readyCount,
                upcomingCount,
                categoryInsights,
                generatedInsights,
                intensityLevel,
                actionableInsights,
                patternRepetitions,
                consequencePoints,
                consequenceMessage,
                totalActivities,
                completedActivities,
                missedActivities,
                activityInsights,
                aiInsightReport,
                heatmapCells,
                weeklyQualityTrend
        );
    }

    private String generateConsequenceMessage(int points) {
        if (points >= 50) return "You're building momentum. Keep going!";
        if (points >= 20) return "Good progress. Stay consistent.";
        if (points >= 0) return "You're holding steady.";
        if (points >= -20) return "⚠️ You're losing control of your decisions.";
        if (points >= -50) return "🚨 CRITICAL: You're in a downward spiral.";
        return "💀 EMERGENCY: Your decision-making has collapsed.";
    }

    private List<String> generateInsights(CategoryInsight worstCategory, String worstTimeLabel,
                                          int highestTimeRegretRate, int readyCount, int goodCount,
                                          int badCount, int totalEvaluated, boolean brutalMode,
                                          int intensityLevel) {
        // Build decision data for LLM
        StringBuilder decisionData = new StringBuilder();
        decisionData.append("Total evaluated decisions: ").append(totalEvaluated).append("\n");
        decisionData.append("Good decisions: ").append(goodCount).append("\n");
        decisionData.append("Bad decisions: ").append(badCount).append("\n");
        decisionData.append("Intensity level (bad decision rate): ").append(intensityLevel).append("%\n");
        if (worstCategory != null) {
            decisionData.append("Worst category: ").append(worstCategory.getCategory())
                    .append(" (").append(worstCategory.getRegretRate()).append("% regret)\n");
        }
        if (worstTimeLabel != null) {
            decisionData.append("Worst time: ").append(worstTimeLabel)
                    .append(" (").append(highestTimeRegretRate).append("% regret)\n");
        }
        decisionData.append("Decisions ready for review: ").append(readyCount).append("\n");

        // Try LLM first
        try {
            java.util.concurrent.CompletableFuture<String> llmFuture = llmService.generateInsights(
                    decisionData.toString(), brutalMode);
            String llmResponse = llmFuture.get(); // Blocking call, but in background thread context
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                List<String> insights = parseInsightsFromLLM(llmResponse);
                if (!insights.isEmpty()) {
                    return insights;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "LLM generation failed, using fallback: " + e.getMessage());
        }

        // Fallback to pre-defined texts
        List<String> insights = new ArrayList<>();

        if (brutalMode) {
            if (worstCategory != null && worstCategory.getRegretRate() >= 50) {
                String[] variations = {
                        "You consistently make bad decisions in " + DateUtils.getCategoryDisplayName(worstCategory.getCategory()).toLowerCase() + ".",
                        "Stop sabotaging yourself in " + DateUtils.getCategoryDisplayName(worstCategory.getCategory()).toLowerCase() + ". Every time is the same.",
                        "Your " + DateUtils.getCategoryDisplayName(worstCategory.getCategory()).toLowerCase() + " decisions are a disaster. Fix it.",
                        "You're destroying yourself through " + DateUtils.getCategoryDisplayName(worstCategory.getCategory()).toLowerCase() + " choices."
                };
                insights.add(variations[ThreadLocalRandom.current().nextInt(variations.length)]);
            }
            if (worstTimeLabel != null && "Late night".equals(worstTimeLabel) && highestTimeRegretRate >= 50) {
                String[] variations = {
                        "You consistently make bad decisions after 11 PM. This is not random — it's a pattern.",
                        "Late night decisions are your kryptonite. You know this, yet you keep doing it.",
                        "Every decision after 11 PM is a mistake you'll regret. Why do you do this?",
                        "Your brain shuts down after 11 PM. Stop making choices then."
                };
                insights.add(variations[ThreadLocalRandom.current().nextInt(variations.length)]);
            }
            if (badCount > goodCount && totalEvaluated > 5) {
                String[] variations = {
                        "This is not random — it's a pattern. You repeat the same mistakes.",
                        "You're in a loop of bad decisions. Break it.",
                        "History repeats itself because you refuse to learn.",
                        "Same mistakes, different day. When will it stop?"
                };
                insights.add(variations[ThreadLocalRandom.current().nextInt(variations.length)]);
            }
            if (intensityLevel >= 60) {
                String[] variations = {
                        "You're in the Self-Sabotage Zone. Time to wake up.",
                        "You're actively working against your own success.",
                        "Self-destruction is not a personality trait. It's a choice you're making.",
                        "You're your own worst enemy right now."
                };
                insights.add(variations[ThreadLocalRandom.current().nextInt(variations.length)]);
            }
        } else {
            if (worstCategory != null) {
                insights.add(
                        "You regret " + DateUtils.getCategoryDisplayName(worstCategory.getCategory()).toLowerCase()
                                + " decisions " + worstCategory.getRegretRate() + "% of the time."
                );
            }
            if (worstTimeLabel != null) {
                if ("Late night".equals(worstTimeLabel)) {
                    insights.add(
                            "Decisions made after 11PM are mostly bad (" + highestTimeRegretRate + "% regret)."
                    );
                } else {
                    insights.add(
                            worstTimeLabel + " decisions carry your highest regret rate at "
                                    + highestTimeRegretRate + "%."
                    );
                }
            }
        }

        if (readyCount > 0) {
            insights.add("You have " + readyCount + " decisions ready for review right now.");
        }
        if (goodCount > badCount && totalEvaluated > 0) {
            insights.add("Your reviewed decisions lean positive overall.");
        }

        return insights;
    }

    private List<String> parseInsightsFromLLM(String llmResponse) {
        List<String> insights = new ArrayList<>();
        String[] lines = llmResponse.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("-") && !trimmed.startsWith("*")) {
                insights.add(trimmed);
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                insights.add(trimmed.substring(1).trim());
            }
        }
        return insights;
    }

    private List<String> generateActionableInsights(CategoryInsight worstCategory, String worstTimeLabel,
                                                     int highestTimeRegretRate, int intensityLevel) {
        // Build decision data for LLM
        StringBuilder decisionData = new StringBuilder();
        if (worstCategory != null) {
            decisionData.append("Worst category: ").append(worstCategory.getCategory())
                    .append(" (").append(worstCategory.getRegretRate()).append("% regret)\n");
        }
        if (worstTimeLabel != null) {
            decisionData.append("Worst time: ").append(worstTimeLabel)
                    .append(" (").append(highestTimeRegretRate).append("% regret)\n");
        }
        decisionData.append("Intensity level (bad decision rate): ").append(intensityLevel).append("%\n");

        // Try LLM first
        try {
            java.util.concurrent.CompletableFuture<String> llmFuture = llmService.generateActionableInsights(
                    decisionData.toString());
            String llmResponse = llmFuture.get();
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                List<String> insights = parseInsightsFromLLM(llmResponse);
                if (!insights.isEmpty()) {
                    return insights;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "LLM generation failed, using fallback: " + e.getMessage());
        }

        // Fallback to pre-defined texts
        List<String> insights = new ArrayList<>();

        if (worstTimeLabel != null && "Late night".equals(worstTimeLabel) && highestTimeRegretRate >= 50) {
            insights.add("Avoid making decisions after 11 PM.");
        }
        if (worstCategory != null && worstCategory.getCategory().equals("health") && 
                worstCategory.getRegretRate() >= 50) {
            insights.add("Delay health decisions by 24 hours.");
        }
        if (worstCategory != null && worstCategory.getCategory().equals("money") && 
                worstCategory.getRegretRate() >= 50) {
            insights.add("Sleep on money decisions for at least one night.");
        }
        if (intensityLevel >= 60) {
            insights.add("Stop making decisions when you're tired or stressed.");
        }

        return insights;
    }

    private List<String> generateActivityInsights(Map<String, Integer> missedByCategory, int missedActivities, int totalActivities) {
        if (totalActivities == 0) {
            return new ArrayList<>();
        }

        double completionRate = totalActivities == 0 ? 0 : 
                ((double) (totalActivities - missedActivities) / totalActivities) * 100;

        // Build activity data for LLM
        StringBuilder activityData = new StringBuilder();
        activityData.append("Total activities: ").append(totalActivities).append("\n");
        activityData.append("Missed activities: ").append(missedActivities).append("\n");
        activityData.append("Completion rate: ").append(String.format("%.1f", completionRate)).append("%\n");
        if (!missedByCategory.isEmpty()) {
            activityData.append("Missed by category:\n");
            for (Map.Entry<String, Integer> entry : missedByCategory.entrySet()) {
                activityData.append("  - ").append(entry.getKey())
                        .append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Try LLM first
        try {
            java.util.concurrent.CompletableFuture<String> llmFuture = llmService.generateActivityInsights(
                    activityData.toString());
            String llmResponse = llmFuture.get();
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                List<String> insights = parseInsightsFromLLM(llmResponse);
                if (!insights.isEmpty()) {
                    return insights;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "LLM generation failed, using fallback: " + e.getMessage());
        }

        // Fallback to pre-defined texts
        List<String> insights = new ArrayList<>();

        if (missedActivities > 0) {
            if (missedActivities >= 3) {
                insights.add("Your execution is weak, not your decisions. You're missing " + missedActivities + " activities.");
            } else {
                insights.add("You've missed " + missedActivities + " activities. Execution gap detected.");
            }

            // Category-specific insights
            for (Map.Entry<String, Integer> entry : missedByCategory.entrySet()) {
                String category = entry.getKey();
                int count = entry.getValue();
                
                if (count >= 2) {
                    switch (category) {
                        case "health":
                            insights.add("You avoid health-related commitments. This is a dangerous pattern.");
                            break;
                        case "study":
                            insights.add("You're consistently skipping study sessions. This affects your long-term growth.");
                            break;
                        case "work":
                            insights.add("Work commitments are being ignored. Professional impact likely.");
                            break;
                        case "personal":
                            insights.add("Personal activities are being deprioritized. Self-care is suffering.");
                            break;
                    }
                }
            }

            if (completionRate < 50) {
                insights.add("Activity completion rate below 50%. Your planning is disconnected from reality.");
            }
        } else if (totalActivities >= 5 && completionRate == 100) {
            insights.add("Excellent execution. You're following through on all planned activities.");
        }

        return insights;
    }

    private List<String> generateCauseEffectInsights(List<DecisionRecord> records, Map<String, Integer> missedByCategory, int missedActivities) {
        List<String> insights = new ArrayList<>();

        if (missedActivities == 0) {
            return insights;
        }

        // Check for cause-effect relationship between missed activities and bad decisions
        for (Map.Entry<String, Integer> entry : missedByCategory.entrySet()) {
            String category = entry.getKey();
            int missedCount = entry.getValue();

            if (missedCount >= 3) {
                // Get bad decisions in same category
                int badDecisionsInCategory = 0;
                for (DecisionRecord record : records) {
                    if (record.isEvaluated() && 
                        record.getDecision().getOutcome() != null && 
                        record.getDecision().getOutcome().equals("bad") &&
                        record.getDecision().getCategory().equals(category)) {
                        badDecisionsInCategory++;
                    }
                }

                if (badDecisionsInCategory >= 2) {
                    insights.add("You missed " + missedCount + " " + category + " activities → your last " + 
                            badDecisionsInCategory + " " + category + " decisions were bad. This is connected.");
                }
            }
        }

        return insights;
    }

    private List<String> prioritizeInsights(List<String> insights, String patternMessage) {
        if (insights.isEmpty()) {
            return insights;
        }

        List<String> prioritized = new ArrayList<>();
        
        // Priority 1: Pattern detection messages (highest urgency)
        for (String insight : insights) {
            if (insight.contains("pattern") || insight.contains("Pattern") || 
                insight.equals(patternMessage) || insight.contains("regain control")) {
                prioritized.add(insight);
            }
        }
        
        // Priority 2: Cause-effect insights (undeniable connections)
        for (String insight : insights) {
            if (insight.contains("→") || insight.contains("connected")) {
                if (!prioritized.contains(insight)) {
                    prioritized.add(insight);
                }
            }
        }
        
        // Priority 3: General activity insights
        for (String insight : insights) {
            if (!prioritized.contains(insight)) {
                prioritized.add(insight);
            }
        }
        
        // Return only top 3 insights to prevent overwhelm
        if (prioritized.size() > 3) {
            return prioritized.subList(0, 3);
        }
        
        return prioritized;
    }

    private LLMService.BehavioralContext buildBehavioralContext(
            int totalEvaluated,
            int goodCount,
            int badCount,
            int neutralCount,
            int readyCount,
            int upcomingCount,
            int activityCompletionRate,
            int completedActivities,
            int missedActivities,
            int goodStreak,
            int badStreak,
            int behaviorScore,
            int riskScore,
            int disciplineScore,
            int highestTimeRegretRate,
            CategoryInsight worstCategory,
            String worstTimeLabel,
            List<PatternRepetition> patternRepetitions,
            List<String> activityInsights,
            List<Integer> weeklyQualityTrend
    ) {
        LLMService.BehavioralContext context = new LLMService.BehavioralContext();
        context.totalEvaluated = totalEvaluated;
        context.goodCount = goodCount;
        context.badCount = badCount;
        context.neutralCount = neutralCount;
        context.readyForReviewCount = readyCount;
        context.upcomingCount = upcomingCount;
        context.activityCompletionRate = activityCompletionRate;
        context.completedActivities = completedActivities;
        context.missedActivities = missedActivities;
        context.disciplineStreak = goodStreak;
        context.badStreak = badStreak;
        context.behaviorScore = behaviorScore;
        context.riskScore = riskScore;
        context.disciplineScore = disciplineScore;
        context.lateNightRisk = Math.max(0, highestTimeRegretRate);
        context.worstCategoryRegretRate = worstCategory == null ? 0 : worstCategory.getRegretRate();
        context.worstCategory = worstCategory == null ? "personal" : worstCategory.getCategory();
        context.worstTimeWindow = worstTimeLabel == null ? "Unknown" : worstTimeLabel;
        context.dominantEmotion = resolveDominantEmotion(worstCategory, highestTimeRegretRate, badStreak);
        context.activityCorrelationHint = resolveActivityCorrelationHint(activityCompletionRate, activityInsights);
        context.recoveryMissionHint = resolveRecoveryHint(activityInsights, badStreak, activityCompletionRate);
        context.weeklyMomentum = resolveMomentumLabel(weeklyQualityTrend);
        context.patternHighlights = extractPatternHighlights(patternRepetitions, activityInsights);
        context.weeklyTrend = buildTrendLabels(weeklyQualityTrend);
        return context;
    }

    private List<HeatmapCell> buildHeatmapCells(List<DecisionRecord> records) {
        Map<Long, Integer> dayIntensity = new LinkedHashMap<>();
        long today = getStartOfDay(System.currentTimeMillis());
        for (int i = 27; i >= 0; i--) {
            long day = today - (i * 24L * 60L * 60L * 1000L);
            dayIntensity.put(day, 0);
        }

        for (DecisionRecord record : records) {
            long decisionDay = getStartOfDay(record.getDecision().getDecisionTime());
            if (!dayIntensity.containsKey(decisionDay)) {
                continue;
            }

            int delta = record.isEvaluated()
                    ? ("good".equals(record.getEvaluation().getOutcome()) ? 2 : "bad".equals(record.getEvaluation().getOutcome()) ? -1 : 1)
                    : 1;
            int current = dayIntensity.get(decisionDay);
            dayIntensity.put(decisionDay, Math.max(-2, Math.min(4, current + delta)));
        }

        List<HeatmapCell> cells = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : dayIntensity.entrySet()) {
            int normalized = Math.max(0, Math.min(4, entry.getValue() + 1));
            cells.add(new HeatmapCell(entry.getKey(), normalized, DateUtils.formatDate(entry.getKey())));
        }
        return cells;
    }

    private List<Integer> buildWeeklyTrendPoints(List<DecisionRecord> records) {
        Map<Long, int[]> dayStats = new LinkedHashMap<>();
        long today = getStartOfDay(System.currentTimeMillis());
        for (int i = 6; i >= 0; i--) {
            long day = today - (i * 24L * 60L * 60L * 1000L);
            dayStats.put(day, new int[2]);
        }

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) {
                continue;
            }
            long day = getStartOfDay(record.getEvaluation().getEvaluatedAt());
            int[] stats = dayStats.get(day);
            if (stats == null) {
                continue;
            }
            stats[1]++;
            if ("good".equals(record.getEvaluation().getOutcome())) {
                stats[0]++;
            }
        }

        List<Integer> trend = new ArrayList<>();
        for (int[] stats : dayStats.values()) {
            trend.add(stats[1] == 0 ? 0 : (int) Math.round((stats[0] * 100.0) / stats[1]));
        }
        return trend;
    }

    private List<String> buildTrendLabels(List<Integer> weeklyQualityTrend) {
        List<String> labels = new ArrayList<>();
        for (int score : weeklyQualityTrend) {
            labels.add(score + "%");
        }
        return labels;
    }

    private int calculateCurrentGoodStreak(List<DecisionRecord> records) {
        int streak = 0;
        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) {
                continue;
            }
            if ("good".equals(record.getEvaluation().getOutcome())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateCurrentBadStreak(List<DecisionRecord> records) {
        int streak = 0;
        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) {
                continue;
            }
            if ("bad".equals(record.getEvaluation().getOutcome())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int computeBehaviorScore(int goodCount, int totalEvaluated, int activityCompletionRate, int goodStreak, int badStreak) {
        int qualityScore = totalEvaluated == 0 ? 0 : (int) Math.round((goodCount * 100.0) / totalEvaluated);
        int streakBoost = Math.min(12, goodStreak * 4);
        int badPenalty = Math.min(18, badStreak * 6);
        return clampScore((int) Math.round((qualityScore * 0.65) + (activityCompletionRate * 0.35) + streakBoost - badPenalty));
    }

    private int computeRiskScore(int badCount, int totalEvaluated, int highestTimeRegretRate, int missedActivities, int totalActivities) {
        int badRate = totalEvaluated == 0 ? 0 : (int) Math.round((badCount * 100.0) / totalEvaluated);
        int missedRate = totalActivities == 0 ? 0 : (int) Math.round((missedActivities * 100.0) / totalActivities);
        return clampScore((int) Math.round((badRate * 0.55) + (Math.max(0, highestTimeRegretRate) * 0.25) + (missedRate * 0.20)));
    }

    private int computeDisciplineScore(int behaviorScore, int activityCompletionRate, int goodStreak) {
        return clampScore((int) Math.round((behaviorScore * 0.6) + (activityCompletionRate * 0.3) + Math.min(10, goodStreak * 2)));
    }

    private String resolveDominantEmotion(CategoryInsight worstCategory, int highestTimeRegretRate, int badStreak) {
        if (badStreak >= 3) {
            return "Overloaded";
        }
        if (highestTimeRegretRate >= 60) {
            return "Tired and impulsive";
        }
        if (worstCategory != null && "money".equalsIgnoreCase(worstCategory.getCategory())) {
            return "Reward-seeking";
        }
        if (worstCategory != null && "relationship".equalsIgnoreCase(worstCategory.getCategory())) {
            return "Emotionally reactive";
        }
        return "Mixed";
    }

    private String resolveActivityCorrelationHint(int activityCompletionRate, List<String> activityInsights) {
        if (activityInsights != null) {
            for (String insight : activityInsights) {
                if (insight != null && (insight.contains("connected") || insight.contains("execution"))) {
                    return insight;
                }
            }
        }
        if (activityCompletionRate < 50) {
            return "Execution gaps are likely feeding your lower-quality decisions.";
        }
        return "When your routine holds, your decisions stabilize faster.";
    }

    private String resolveRecoveryHint(List<String> activityInsights, int badStreak, int activityCompletionRate) {
        if (activityInsights != null) {
            for (String insight : activityInsights) {
                if (insight != null && insight.toLowerCase().contains("regain control")) {
                    return insight;
                }
            }
        }
        if (badStreak >= 2) {
            return "Interrupt the spiral with one easy completed activity before any big decision.";
        }
        if (activityCompletionRate < 60) {
            return "Lower the day's friction and finish the easiest task before noon.";
        }
        return "Protect the routines that already keep you steady.";
    }

    private String resolveMomentumLabel(List<Integer> weeklyQualityTrend) {
        if (weeklyQualityTrend == null || weeklyQualityTrend.isEmpty()) {
            return "Your weekly signal is still forming.";
        }
        int latest = weeklyQualityTrend.get(weeklyQualityTrend.size() - 1);
        int previous = weeklyQualityTrend.size() > 1 ? weeklyQualityTrend.get(weeklyQualityTrend.size() - 2) : latest;
        if (latest - previous >= 10) {
            return "Momentum is rising.";
        }
        if (previous - latest >= 10) {
            return "Momentum slipped late in the week.";
        }
        return "Momentum is stable but fragile.";
    }

    private List<String> extractPatternHighlights(List<PatternRepetition> patternRepetitions, List<String> activityInsights) {
        List<String> highlights = new ArrayList<>();
        for (PatternRepetition repetition : patternRepetitions) {
            highlights.add(repetition.getCategory() + " / " + repetition.getTimePattern() + " x" + repetition.getCount());
            if (highlights.size() == 2) {
                break;
            }
        }
        if (activityInsights != null) {
            for (String insight : activityInsights) {
                if (insight == null || insight.trim().isEmpty()) {
                    continue;
                }
                highlights.add(insight);
                if (highlights.size() == 3) {
                    break;
                }
            }
        }
        return highlights;
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private List<PatternRepetition> detectPatternRepetitions(List<DecisionRecord> records) {
        List<PatternRepetition> repetitions = new ArrayList<>();
        Map<String, Map<Integer, Integer>> patternMap = new LinkedHashMap<>();

        for (DecisionRecord record : records) {
            if (!record.isEvaluated()) continue;

            Decision decision = record.getDecision();
            String category = decision.getCategory();
            int hour = decision.getDecisionHour();

            if (category == null) continue;

            patternMap.putIfAbsent(category, new LinkedHashMap<>());
            Map<Integer, Integer> hourMap = patternMap.get(category);
            hourMap.put(hour, hourMap.getOrDefault(hour, 0) + 1);
        }

        for (Map.Entry<String, Map<Integer, Integer>> entry : patternMap.entrySet()) {
            String category = entry.getKey();
            for (Map.Entry<Integer, Integer> hourEntry : entry.getValue().entrySet()) {
                int hour = hourEntry.getKey();
                int count = hourEntry.getValue();

                if (count >= 3) {
                    String timePattern = DateUtils.getHourLabel(hour);
                    String message = "You've made this mistake " + count + " times.";
                    repetitions.add(new PatternRepetition(category, timePattern, count, message));
                }
            }
        }

        return repetitions;
    }

    private List<DecisionRecord> mapDecisionRecords(@NonNull QuerySnapshot snapshot) {
        List<DecisionRecord> records = new ArrayList<>();

        for (QueryDocumentSnapshot document : snapshot) {
            Decision decision = document.toObject(Decision.class);
            if (decision == null) {
                Log.w(TAG, "Skipping null decision for document: " + document.getId());
                continue;
            }

            // Edge case: Handle null userId in document
            String docUserId = decision.getUserId();
            if (docUserId == null) {
                Log.w(TAG, "Skipping decision with null userId: " + document.getId());
                continue;
            }

            if (decision.getId() == 0L) {
                try {
                    decision.setId(Long.parseLong(document.getId()));
                } catch (NumberFormatException ignored) {
                    Log.w(TAG, "Skipping decision with invalid ID: " + document.getId());
                    continue;
                }
            }
            records.add(buildDecisionRecord(decision));
        }
        records.sort(Comparator.comparingLong(
                (DecisionRecord record) -> record.getDecision().getDecisionTime()
        ).reversed());
        return records;
    }

    private DecisionRecord buildDecisionRecord(@NonNull Decision decision) {
        String outcome = decision.getOutcome();
        Long evaluatedAt = decision.getEvaluatedAt();
        
        // Edge case: Handle null or empty outcome
        if (outcome == null || outcome.trim().isEmpty() || evaluatedAt == null) {
            Log.d(TAG, "Decision " + decision.getId() + " is not evaluated yet");
            return new DecisionRecord(decision, null);
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setDecisionId(decision.getId());
        evaluation.setOutcome(outcome);
        evaluation.setReflectionNotes(
                decision.getReflectionNotes() == null ? "" : decision.getReflectionNotes()
        );
        evaluation.setEvaluatedAt(evaluatedAt);
        return new DecisionRecord(decision, evaluation);
    }

    private String requireUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Please log in to continue.");
        }
        return currentUser.getUid();
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            throw new RuntimeException(
                    cause != null && cause.getMessage() != null
                            ? cause.getMessage()
                            : "We couldn't complete that action.",
                    cause != null ? cause : exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The request was interrupted.", exception);
        }
    }

    private com.google.firebase.firestore.DocumentReference decisionDocument(long decisionId) {
        return firestore.collection("decisions").document(String.valueOf(decisionId));
    }
}
