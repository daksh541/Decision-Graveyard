package com.sai.decisiongraveyard.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.DecisionRepository;

import java.util.Calendar;

public class SmartNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "SmartNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if ("daily_summary".equals(action)) {
            sendDailySummary(context);
        } else if ("weekly_report".equals(action)) {
            sendWeeklyReport(context);
        }
    }

    private void sendDailySummary(Context context) {
        new Thread(() -> {
            ActivityRepository activityRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getActivityRepository();
            DecisionRepository decisionRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getDecisionRepository();

            // Get today's data
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long dayStart = calendar.getTimeInMillis();

            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            long dayEnd = calendar.getTimeInMillis();

            try {
                int activitiesCompleted = activityRepo.getActivitiesForDay(dayStart, dayEnd).size();
                java.util.List<com.sai.decisiongraveyard.model.DecisionRecord> records = decisionRepo.getAllDecisionRecords();
                
                int decisionsMade = 0;
                int score = 0;
                
                for (com.sai.decisiongraveyard.model.DecisionRecord record : records) {
                    long decisionTime = record.getDecision().getDecisionTime();
                    if (decisionTime >= dayStart && decisionTime <= dayEnd) {
                        decisionsMade++;
                        if ("good".equals(record.getDecision().getOutcome())) {
                            score += 10;
                        } else if ("bad".equals(record.getDecision().getOutcome())) {
                            score -= 10;
                        }
                    }
                }
                
                SmartNotificationScheduler.showDailySummaryNotification(
                        context, decisionsMade, activitiesCompleted, score);
            } catch (Exception e) {
                Log.e(TAG, "Error sending daily summary: " + e.getMessage(), e);
            }
        }).start();
    }

    private void sendWeeklyReport(Context context) {
        new Thread(() -> {
            ActivityRepository activityRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getActivityRepository();
            DecisionRepository decisionRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getDecisionRepository();

            // Get this week's data
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long weekStart = calendar.getTimeInMillis();

            try {
                java.util.List<com.sai.decisiongraveyard.model.Activity> activities = activityRepo.getAllActivities();
                int completedActivities = 0;
                int missedActivities = 0;
                
                for (com.sai.decisiongraveyard.model.Activity activity : activities) {
                    if (activity.getScheduledTime() >= weekStart) {
                        if ("completed".equals(activity.getStatus())) {
                            completedActivities++;
                        } else if ("missed".equals(activity.getStatus())) {
                            missedActivities++;
                        }
                    }
                }
                
                java.util.List<com.sai.decisiongraveyard.model.DecisionRecord> records = decisionRepo.getAllDecisionRecords();
                int goodDecisions = 0;
                int badDecisions = 0;
                
                for (com.sai.decisiongraveyard.model.DecisionRecord record : records) {
                    long decisionTime = record.getDecision().getDecisionTime();
                    if (decisionTime >= weekStart) {
                        if ("good".equals(record.getDecision().getOutcome())) {
                            goodDecisions++;
                        } else if ("bad".equals(record.getDecision().getOutcome())) {
                            badDecisions++;
                        }
                    }
                }
                
                SmartNotificationScheduler.showWeeklyReportNotification(
                        context, goodDecisions, badDecisions, completedActivities, missedActivities);
            } catch (Exception e) {
                Log.e(TAG, "Error sending weekly report: " + e.getMessage(), e);
            }
        }).start();
    }
}
