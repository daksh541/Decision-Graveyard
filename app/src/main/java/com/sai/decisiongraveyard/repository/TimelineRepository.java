package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.model.TimelineEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimelineRepository {

    private static final String TAG = "TimelineRepository";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ActivityRepository activityRepository;
    private DecisionRepository decisionRepository;
    private Context context;

    public TimelineRepository(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        RepositoryProvider provider = RepositoryProvider.getInstance(context);
        activityRepository = provider.getActivityRepository();
        decisionRepository = provider.getDecisionRepository();
    }

    public interface TimelineCallback {
        void onSuccess(List<TimelineDay> days);
        void onError(String error);
    }

    public String requireUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return auth.getCurrentUser().getUid();
    }

    public void getTimeline(int days, TimelineCallback callback) {
        new Thread(() -> {
            try {
                String userId = requireUserId();
                long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

                List<Activity> activities = activityRepository.getAllActivities();
                List<DecisionRecord> decisions = decisionRepository.getAllDecisionRecords();
                
                List<TimelineDay> timeline = buildTimeline(activities, decisions, cutoffTime);
                callback.onSuccess(timeline);
            } catch (Exception e) {
                Log.e(TAG, "Error building timeline", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private List<TimelineDay> buildTimeline(List<Activity> activities, 
                                             List<DecisionRecord> decisions,
                                             long cutoffTime) {
        Map<String, TimelineDay> dayMap = new HashMap<>();
        
        // Process activities
        for (Activity activity : activities) {
            if (activity.getScheduledTime() >= cutoffTime) {
                String dayKey = getDayKey(activity.getScheduledTime());
                TimelineDay day = dayMap.getOrDefault(dayKey, new TimelineDay(dayKey, activity.getScheduledTime()));
                
                TimelineEvent event = TimelineEvent.fromActivity(activity);
                day.events.add(event);
                day.activityCount++;
                
                if ("completed".equals(event.getStatus())) {
                    day.completedActivities++;
                } else if ("missed".equals(event.getStatus())) {
                    day.missedActivities++;
                }
                
                dayMap.put(dayKey, day);
            }
        }
        
        // Process decisions
        for (DecisionRecord record : decisions) {
            Decision decision = record.getDecision();
            if (decision.getDecisionTime() >= cutoffTime) {
                String dayKey = getDayKey(decision.getDecisionTime());
                TimelineDay day = dayMap.getOrDefault(dayKey, new TimelineDay(dayKey, decision.getDecisionTime()));
                
                TimelineEvent event = TimelineEvent.fromDecisionRecord(record);
                day.events.add(event);
                day.decisionCount++;
                
                if ("good".equals(event.getStatus())) {
                    day.goodDecisions++;
                } else if ("bad".equals(event.getStatus())) {
                    day.badDecisions++;
                }
                
                dayMap.put(dayKey, day);
            }
        }
        
        // Convert to list and sort by date descending
        List<TimelineDay> timeline = new ArrayList<>(dayMap.values());
        timeline.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        
        // Calculate scores for each day
        for (TimelineDay day : timeline) {
            day.calculateScore();
        }
        
        return timeline;
    }

    private String getDayKey(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class TimelineDay {
        public String dateKey;
        public long timestamp;
        public List<TimelineEvent> events;
        public int decisionCount;
        public int activityCount;
        public int goodDecisions;
        public int badDecisions;
        public int completedActivities;
        public int missedActivities;
        public int score;
        public double completionRate;

        public TimelineDay(String dateKey, long timestamp) {
            this.dateKey = dateKey;
            this.timestamp = timestamp;
            this.events = new ArrayList<>();
            this.decisionCount = 0;
            this.activityCount = 0;
            this.goodDecisions = 0;
            this.badDecisions = 0;
            this.completedActivities = 0;
            this.missedActivities = 0;
            this.score = 0;
            this.completionRate = 0;
        }

        public void calculateScore() {
            // Score calculation: +10 for good decision, -10 for bad, +15 for completed, -10 for missed
            int decisionScore = (goodDecisions * 10) - (badDecisions * 10);
            int activityScore = (completedActivities * 15) - (missedActivities * 10);
            this.score = decisionScore + activityScore;
            
            // Calculate completion rate
            int totalActivities = completedActivities + missedActivities;
            if (totalActivities > 0) {
                this.completionRate = (double) completedActivities / totalActivities * 100;
            }
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        public String getRelativeDate() {
            Calendar today = Calendar.getInstance();
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTimeInMillis(timestamp);
            
            if (isSameDay(today, eventDate)) {
                return "Today";
            } else if (isYesterday(today, eventDate)) {
                return "Yesterday";
            } else {
                return getFormattedDate();
            }
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

        private boolean isYesterday(Calendar today, Calendar eventDate) {
            Calendar yesterday = (Calendar) today.clone();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            return isSameDay(yesterday, eventDate);
        }
    }

    private <T> T await(Task<T> task) {
        try {
            return Tasks.await(task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
