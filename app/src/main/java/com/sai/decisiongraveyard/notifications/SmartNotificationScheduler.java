package com.sai.decisiongraveyard.notifications;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.DecisionRepository;
import com.sai.decisiongraveyard.repository.UserPreferencesRepository;

import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SmartNotificationScheduler {

    private static final String CHANNEL_ID_DAILY = "daily_summary_channel";
    private static final String CHANNEL_ID_WEEKLY = "weekly_report_channel";
    private static final String CHANNEL_ID_BEHAVIORAL = "behavioral_trigger_channel";
    private static final String UNIQUE_BEHAVIORAL_WORK = "behavioral_trigger_worker";
    private static final String TAG = "SmartNotificationScheduler";

    public static void scheduleDailySummary(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Skipping daily summary scheduling because no user is signed in");
            return;
        }
        UserPreferencesRepository preferencesRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getUserPreferencesRepository();
        preferencesRepo.getUserPreferences(new UserPreferencesRepository.PreferencesCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserPreferences preferences) {
                if (!preferences.isDailySummaryEnabled()) {
                    Log.d(TAG, "Daily summary disabled");
                    return;
                }

                // Schedule for 9 PM daily
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 21);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                long triggerTime = calendar.getTimeInMillis();
                long interval = AlarmManager.INTERVAL_DAY;

                scheduleRepeatingAlarm(context, triggerTime, interval, 
                        "daily_summary", CHANNEL_ID_DAILY, "Daily Summary");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting preferences for daily summary: " + error);
            }
        });
    }

    public static void scheduleWeeklyReport(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Skipping weekly report scheduling because no user is signed in");
            return;
        }
        UserPreferencesRepository preferencesRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getUserPreferencesRepository();
        preferencesRepo.getUserPreferences(new UserPreferencesRepository.PreferencesCallback() {
            @Override
            public void onSuccess(com.sai.decisiongraveyard.model.UserPreferences preferences) {
                if (!preferences.isWeeklyReportEnabled()) {
                    Log.d(TAG, "Weekly report disabled");
                    return;
                }

                // Schedule for Sunday 10 AM
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 10);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                }

                long triggerTime = calendar.getTimeInMillis();
                long interval = AlarmManager.INTERVAL_DAY * 7;

                scheduleRepeatingAlarm(context, triggerTime, interval,
                        "weekly_report", CHANNEL_ID_WEEKLY, "Weekly Report");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting preferences for weekly report: " + error);
            }
        });
    }

    public static void scheduleBehavioralTrigger(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "Skipping behavioral trigger scheduling because no user is signed in");
            return;
        }
        // Schedule periodic check for behavioral triggers using WorkManager
        PeriodicWorkRequest behavioralCheck = new PeriodicWorkRequest.Builder(
                BehavioralTriggerWorker.class,
                6, TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_BEHAVIORAL_WORK,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                behavioralCheck
        );
        Log.d(TAG, "Behavioral trigger worker scheduled");
    }

    private static void scheduleRepeatingAlarm(Context context, long triggerTime, long interval,
                                              String action, String channelId, String channelName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        createNotificationChannel(context, channelId, channelName);

        Intent intent = new Intent(context, SmartNotificationReceiver.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, interval, pendingIntent);
                return;
            }
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, interval, pendingIntent);
        Log.d(TAG, "Scheduled " + action + " for: " + new java.util.Date(triggerTime));
    }

    public static void createNotificationChannel(Context context, String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(channelName + " notifications");
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showDailySummaryNotification(Context context, int decisionsMade, int activitiesCompleted, int score) {
        createNotificationChannel(context, CHANNEL_ID_DAILY, "Daily Summary");

        String title = "Daily Summary";
        String message = String.format("You made %d decisions and completed %d activities. Score: %d",
                decisionsMade, activitiesCompleted, score);

        showNotification(context, CHANNEL_ID_DAILY, title, message);
    }

    public static void showWeeklyReportNotification(Context context, int goodDecisions, int badDecisions, 
                                                   int completedActivities, int missedActivities) {
        createNotificationChannel(context, CHANNEL_ID_WEEKLY, "Weekly Report");

        String title = "Weekly Report";
        String message = String.format("This week: %d good, %d bad decisions. %d completed, %d missed activities.",
                goodDecisions, badDecisions, completedActivities, missedActivities);

        showNotification(context, CHANNEL_ID_WEEKLY, title, message);
    }

    public static void showBehavioralTriggerNotification(Context context, String triggerType, String message) {
        createNotificationChannel(context, CHANNEL_ID_BEHAVIORAL, "Behavioral Triggers");

        String title;
        switch (triggerType) {
            case "level_up":
                title = "Level Up!";
                break;
            case "streak_milestone":
                title = "Streak Milestone!";
                break;
            case "pattern_warning":
                title = "Pattern Detected";
                break;
            case "motivation":
                title = "Keep Going!";
                break;
            default:
                title = "Behavioral Insight";
        }

        showNotification(context, CHANNEL_ID_BEHAVIORAL, title, message);
    }

    private static void showNotification(Context context, String channelId, String title, String message) {
        Intent intent = new Intent(context, com.sai.decisiongraveyard.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ThreadLocalRandom.current().nextInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
