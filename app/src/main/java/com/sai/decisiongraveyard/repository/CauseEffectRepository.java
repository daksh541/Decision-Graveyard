package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.model.CauseEffectInsight;
import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CauseEffectRepository {

    private static final String TAG = "CauseEffectRepository";
    private static final String COLLECTION_NAME = "causeEffectInsights";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ActivityRepository activityRepository;
    private DecisionRepository decisionRepository;

    public CauseEffectRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void setActivityRepository(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public void setDecisionRepository(DecisionRepository decisionRepository) {
        this.decisionRepository = decisionRepository;
    }

    public interface InsightCallback {
        void onSuccess(List<CauseEffectInsight> insights);
        void onError(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String error);
    }

    public String requireUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return auth.getCurrentUser().getUid();
    }

    public void analyzeCauseEffectPatterns(int daysToAnalyze, InsightCallback callback) {
        new Thread(() -> {
            try {
                String userId = requireUserId();
                long cutoffTime = System.currentTimeMillis() - (daysToAnalyze * 24L * 60 * 60 * 1000);

                List<Activity> activities = activityRepository.getAllActivities();
                List<DecisionRecord> decisions = decisionRepository.getAllDecisionRecords();
                
                List<CauseEffectInsight> insights = generateInsights(activities, decisions, cutoffTime, userId);
                saveInsights(insights, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing patterns", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private List<CauseEffectInsight> generateInsights(List<Activity> activities, 
                                                      List<DecisionRecord> decisions, 
                                                      long cutoffTime, 
                                                      String userId) {
        List<CauseEffectInsight> insights = new ArrayList<>();

        // Filter activities and decisions within time range
        List<Activity> recentActivities = filterByTime(activities, cutoffTime);
        List<DecisionRecord> recentDecisions = filterDecisionsByTime(decisions, cutoffTime);

        // Analyze by category
        Map<String, CategoryStats> categoryStats = analyzeByCategory(recentActivities, recentDecisions);

        // Generate insights for each category
        for (Map.Entry<String, CategoryStats> entry : categoryStats.entrySet()) {
            String category = entry.getKey();
            CategoryStats stats = entry.getValue();

            if (stats.missedActivities >= 3 && stats.badDecisions >= 2) {
                String message = String.format(
                    "Your missed %s activities are affecting your decision quality. " +
                    "%d missed activities correlate with %d bad decisions.",
                    category, stats.missedActivities, stats.badDecisions
                );
                
                CauseEffectInsight insight = new CauseEffectInsight(
                    userId, category, "negative_correlation", 
                    message, stats.missedActivities, stats.badDecisions
                );
                insights.add(insight);
            } else if (stats.completedActivities >= 5 && stats.goodDecisions >= 3) {
                String message = String.format(
                    "Great job! Your %s discipline is paying off. " +
                    "%d completed activities correlate with %d good decisions.",
                    category, stats.completedActivities, stats.goodDecisions
                );
                
                CauseEffectInsight insight = new CauseEffectInsight(
                    userId, category, "positive_correlation", 
                    message, stats.completedActivities, stats.goodDecisions
                );
                insights.add(insight);
            }
        }

        // Time-based analysis
        analyzeTimePatterns(recentActivities, recentDecisions, insights, userId);

        return insights;
    }

    private List<Activity> filterByTime(List<Activity> activities, long cutoffTime) {
        List<Activity> filtered = new ArrayList<>();
        for (Activity activity : activities) {
            if (activity.getScheduledTime() >= cutoffTime) {
                filtered.add(activity);
            }
        }
        return filtered;
    }

    private List<DecisionRecord> filterDecisionsByTime(List<DecisionRecord> decisions, long cutoffTime) {
        List<DecisionRecord> filtered = new ArrayList<>();
        for (DecisionRecord record : decisions) {
            if (record.getDecision().getDecisionTime() >= cutoffTime) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private Map<String, CategoryStats> analyzeByCategory(List<Activity> activities, 
                                                         List<DecisionRecord> decisions) {
        Map<String, CategoryStats> stats = new HashMap<>();

        for (Activity activity : activities) {
            String category = activity.getCategory();
            CategoryStats catStats = stats.getOrDefault(category, new CategoryStats());
            
            if ("missed".equals(activity.getStatus())) {
                catStats.missedActivities++;
            } else if ("completed".equals(activity.getStatus())) {
                catStats.completedActivities++;
            }
            
            stats.put(category, catStats);
        }

        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            String category = decision.getCategory();
            CategoryStats catStats = stats.getOrDefault(category, new CategoryStats());
            
            if ("bad".equals(decision.getOutcome())) {
                catStats.badDecisions++;
            } else if ("good".equals(decision.getOutcome())) {
                catStats.goodDecisions++;
            }
            
            stats.put(category, catStats);
        }

        return stats;
    }

    private void analyzeTimePatterns(List<Activity> activities, 
                                    List<DecisionRecord> decisions,
                                    List<CauseEffectInsight> insights,
                                    String userId) {
        // Analyze if missed activities precede bad decisions
        Map<String, Integer> timeWindowMisses = new HashMap<>();
        Map<String, Integer> timeWindowBadDecisions = new HashMap<>();

        for (Activity activity : activities) {
            if ("missed".equals(activity.getStatus())) {
                String timeWindow = getTimeWindow(activity.getScheduledTime());
                timeWindowMisses.put(timeWindow, timeWindowMisses.getOrDefault(timeWindow, 0) + 1);
            }
        }

        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            if ("bad".equals(decision.getOutcome())) {
                String timeWindow = getTimeWindow(decision.getDecisionTime());
                timeWindowBadDecisions.put(timeWindow, timeWindowBadDecisions.getOrDefault(timeWindow, 0) + 1);
            }
        }

        // Check for patterns
        for (String window : timeWindowMisses.keySet()) {
            int misses = timeWindowMisses.get(window);
            int badDecs = timeWindowBadDecisions.getOrDefault(window, 0);
            
            if (misses >= 3 && badDecs >= 2) {
                String message = String.format(
                    "Pattern detected: During %s, you missed %d activities and made %d bad decisions. " +
                    "Focus on completing tasks during this time.",
                    window, misses, badDecs
                );
                
                CauseEffectInsight insight = new CauseEffectInsight(
                    userId, "time_pattern", "temporal_correlation",
                    message, misses, badDecs
                );
                insights.add(insight);
            }
        }
    }

    private String getTimeWindow(long timestamp) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) return "Morning (5AM-12PM)";
        if (hour >= 12 && hour < 17) return "Afternoon (12PM-5PM)";
        if (hour >= 17 && hour < 21) return "Evening (5PM-9PM)";
        return "Night (9PM-5AM)";
    }

    private void saveInsights(List<CauseEffectInsight> insights, InsightCallback callback) {
        if (insights.isEmpty()) {
            callback.onSuccess(insights);
            return;
        }

        final int[] saved = {0};
        for (CauseEffectInsight insight : insights) {
            db.collection(COLLECTION_NAME)
                    .add(insight)
                    .addOnSuccessListener(documentReference -> {
                        saved[0]++;
                        if (saved[0] == insights.size()) {
                            callback.onSuccess(insights);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving insight", e);
                        callback.onError(e.getMessage());
                    });
        }
    }

    public void getRecentInsights(InsightCallback callback) {
        String userId = requireUserId();
        long cutoffTime = System.currentTimeMillis() - (7 * 24L * 60 * 60 * 1000);

        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("generatedAt", cutoffTime)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<CauseEffectInsight> insights = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        CauseEffectInsight insight = doc.toObject(CauseEffectInsight.class);
                        if (insight != null) {
                            insight.setInsightId(doc.getId());
                            insights.add(insight);
                        }
                    }
                    callback.onSuccess(insights);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting recent insights", e);
                    callback.onError(e.getMessage());
                });
    }

    private static class CategoryStats {
        int missedActivities = 0;
        int completedActivities = 0;
        int badDecisions = 0;
        int goodDecisions = 0;
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
