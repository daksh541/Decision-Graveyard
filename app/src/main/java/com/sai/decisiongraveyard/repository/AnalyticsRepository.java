package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsRepository {

    private static final String TAG = "AnalyticsRepository";

    private FirebaseAuth auth;
    private ActivityRepository activityRepository;
    private DecisionRepository decisionRepository;
    private Context context;

    public AnalyticsRepository(Context context) {
        this.context = context.getApplicationContext();
        auth = FirebaseAuth.getInstance();
        RepositoryProvider provider = RepositoryProvider.getInstance(context);
        activityRepository = provider.getActivityRepository();
        decisionRepository = provider.getDecisionRepository();
    }

    public interface AnalyticsCallback {
        void onSuccess(AdvancedAnalytics analytics);
        void onError(String error);
    }

    public String requireUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return auth.getCurrentUser().getUid();
    }

    public void getAdvancedAnalytics(int weeks, AnalyticsCallback callback) {
        new Thread(() -> {
            try {
                String userId = requireUserId();
                long cutoffTime = System.currentTimeMillis() - (weeks * 7L * 24 * 60 * 60 * 1000);

                List<Activity> activities = activityRepository.getAllActivities();
                List<DecisionRecord> decisions = decisionRepository.getAllDecisionRecords();
                
                AdvancedAnalytics analytics = generateAnalytics(activities, decisions, cutoffTime);
                callback.onSuccess(analytics);
            } catch (Exception e) {
                Log.e(TAG, "Error generating advanced analytics", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private AdvancedAnalytics generateAnalytics(List<Activity> activities, 
                                                 List<DecisionRecord> decisions,
                                                 long cutoffTime) {
        AdvancedAnalytics analytics = new AdvancedAnalytics();
        
        // Filter by time
        List<Activity> recentActivities = filterByTime(activities, cutoffTime);
        List<DecisionRecord> recentDecisions = filterDecisionsByTime(decisions, cutoffTime);

        // Weekly trends
        analytics.weeklyTrends = calculateWeeklyTrends(recentActivities, recentDecisions);
        
        // Best and worst categories
        analytics.bestCategories = findBestCategories(recentDecisions);
        analytics.worstCategories = findWorstCategories(recentDecisions);
        
        // Peak failure times
        analytics.peakFailureTimes = findPeakFailureTimes(recentActivities);
        
        // Improvement tracking
        analytics.improvementScore = calculateImprovementScore(recentActivities, recentDecisions);

        return analytics;
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

    private List<WeeklyData> calculateWeeklyTrends(List<Activity> activities, List<DecisionRecord> decisions) {
        List<WeeklyData> trends = new ArrayList<>();
        Map<Integer, WeekStats> weekMap = new HashMap<>();

        for (Activity activity : activities) {
            int weekNumber = getWeekNumber(activity.getScheduledTime());
            WeekStats stats = weekMap.getOrDefault(weekNumber, new WeekStats());
            
            if ("completed".equals(activity.getStatus())) {
                stats.completedActivities++;
            } else if ("missed".equals(activity.getStatus())) {
                stats.missedActivities++;
            }
            
            weekMap.put(weekNumber, stats);
        }

        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            int weekNumber = getWeekNumber(decision.getDecisionTime());
            WeekStats stats = weekMap.getOrDefault(weekNumber, new WeekStats());
            
            if ("good".equals(decision.getOutcome())) {
                stats.goodDecisions++;
            } else if ("bad".equals(decision.getOutcome())) {
                stats.badDecisions++;
            }
            
            weekMap.put(weekNumber, stats);
        }

        // Convert to list and sort
        for (Map.Entry<Integer, WeekStats> entry : weekMap.entrySet()) {
            WeeklyData data = new WeeklyData();
            data.weekNumber = entry.getKey();
            data.stats = entry.getValue();
            trends.add(data);
        }

        trends.sort((a, b) -> Integer.compare(a.weekNumber, b.weekNumber));
        return trends;
    }

    private List<CategoryPerformance> findBestCategories(List<DecisionRecord> decisions) {
        Map<String, CategoryStats> categoryMap = new HashMap<>();

        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            String category = decision.getCategory();
            CategoryStats stats = categoryMap.getOrDefault(category, new CategoryStats());
            
            if ("good".equals(decision.getOutcome())) {
                stats.good++;
            } else if ("bad".equals(decision.getOutcome())) {
                stats.bad++;
            }
            
            stats.total++;
            categoryMap.put(category, stats);
        }

        List<CategoryPerformance> best = new ArrayList<>();
        for (Map.Entry<String, CategoryStats> entry : categoryMap.entrySet()) {
            CategoryStats stats = entry.getValue();
            if (stats.total >= 3) {
                double rate = (double) stats.good / stats.total;
                if (rate >= 0.7) {
                    CategoryPerformance perf = new CategoryPerformance();
                    perf.category = entry.getKey();
                    perf.goodRate = rate;
                    perf.total = stats.total;
                    best.add(perf);
                }
            }
        }

        best.sort((a, b) -> Double.compare(b.goodRate, a.goodRate));
        return best;
    }

    private List<CategoryPerformance> findWorstCategories(List<DecisionRecord> decisions) {
        Map<String, CategoryStats> categoryMap = new HashMap<>();

        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            String category = decision.getCategory();
            CategoryStats stats = categoryMap.getOrDefault(category, new CategoryStats());
            
            if ("good".equals(decision.getOutcome())) {
                stats.good++;
            } else if ("bad".equals(decision.getOutcome())) {
                stats.bad++;
            }
            
            stats.total++;
            categoryMap.put(category, stats);
        }

        List<CategoryPerformance> worst = new ArrayList<>();
        for (Map.Entry<String, CategoryStats> entry : categoryMap.entrySet()) {
            CategoryStats stats = entry.getValue();
            if (stats.total >= 3) {
                double rate = (double) stats.bad / stats.total;
                if (rate >= 0.5) {
                    CategoryPerformance perf = new CategoryPerformance();
                    perf.category = entry.getKey();
                    perf.badRate = rate;
                    perf.total = stats.total;
                    worst.add(perf);
                }
            }
        }

        worst.sort((a, b) -> Double.compare(b.badRate, a.badRate));
        return worst;
    }

    private List<FailureTimeSlot> findPeakFailureTimes(List<Activity> activities) {
        Map<String, Integer> timeSlotMisses = new HashMap<>();

        for (Activity activity : activities) {
            if ("missed".equals(activity.getStatus())) {
                String timeSlot = getTimeSlot(activity.getScheduledTime());
                timeSlotMisses.put(timeSlot, timeSlotMisses.getOrDefault(timeSlot, 0) + 1);
            }
        }

        List<FailureTimeSlot> peakTimes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : timeSlotMisses.entrySet()) {
            if (entry.getValue() >= 3) {
                FailureTimeSlot slot = new FailureTimeSlot();
                slot.timeSlot = entry.getKey();
                slot.missCount = entry.getValue();
                peakTimes.add(slot);
            }
        }

        peakTimes.sort((a, b) -> Integer.compare(b.missCount, a.missCount));
        return peakTimes;
    }

    private double calculateImprovementScore(List<Activity> activities, List<DecisionRecord> decisions) {
        // Calculate recent vs earlier performance
        long midPoint = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000); // 2 weeks ago
        
        int recentCompleted = 0, recentMissed = 0;
        int earlierCompleted = 0, earlierMissed = 0;

        for (Activity activity : activities) {
            if (activity.getScheduledTime() >= midPoint) {
                if ("completed".equals(activity.getStatus())) recentCompleted++;
                else if ("missed".equals(activity.getStatus())) recentMissed++;
            } else {
                if ("completed".equals(activity.getStatus())) earlierCompleted++;
                else if ("missed".equals(activity.getStatus())) earlierMissed++;
            }
        }

        double recentRate = recentCompleted + recentMissed > 0 ? 
                (double) recentCompleted / (recentCompleted + recentMissed) : 0;
        double earlierRate = earlierCompleted + earlierMissed > 0 ? 
                (double) earlierCompleted / (earlierCompleted + earlierMissed) : 0;

        return (recentRate - earlierRate) * 100; // Percentage improvement
    }

    private int getWeekNumber(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR);
    }

    private String getTimeSlot(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 9) return "Early Morning (5-9 AM)";
        if (hour >= 9 && hour < 12) return "Late Morning (9 AM-12 PM)";
        if (hour >= 12 && hour < 14) return "Noon (12-2 PM)";
        if (hour >= 14 && hour < 17) return "Afternoon (2-5 PM)";
        if (hour >= 17 && hour < 20) return "Evening (5-8 PM)";
        if (hour >= 20 && hour < 23) return "Night (8-11 PM)";
        return "Late Night (11 PM-5 AM)";
    }

    public static class AdvancedAnalytics {
        public List<WeeklyData> weeklyTrends;
        public List<CategoryPerformance> bestCategories;
        public List<CategoryPerformance> worstCategories;
        public List<FailureTimeSlot> peakFailureTimes;
        public double improvementScore;
    }

    public static class WeeklyData {
        public int weekNumber;
        public WeekStats stats;
    }

    public static class WeekStats {
        public int completedActivities = 0;
        public int missedActivities = 0;
        public int goodDecisions = 0;
        public int badDecisions = 0;
    }

    public static class CategoryStats {
        public int good = 0;
        public int bad = 0;
        public int total = 0;
    }

    public static class CategoryPerformance {
        public String category;
        public double goodRate;
        public double badRate;
        public int total;
    }

    public static class FailureTimeSlot {
        public String timeSlot;
        public int missCount;
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
