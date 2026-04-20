package com.sai.decisiongraveyard.notifications;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.sai.decisiongraveyard.R;

public final class NotificationScheduler {

    public static final String CHANNEL_ID = "decision_review_channel";
    public static final String EXTRA_DECISION_ID = "extra_decision_id";
    public static final String EXTRA_DECISION_TITLE = "extra_decision_title";

    private static final String TAG = "NotificationScheduler";

    private NotificationScheduler() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        notificationManager.createNotificationChannel(channel);
    }

    public static void scheduleReminder(Context context, long decisionId, String title, long evaluationTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.w(TAG, "AlarmManager is null");
            return;
        }

        // Check for SCHEDULE_EXACT_ALARM permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted. Using inexact alarm.");
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        Math.max(evaluationTime, System.currentTimeMillis() + 1000L),
                        buildPendingIntent(context, decisionId, title)
                );
                return;
            }
        }

        long triggerAtMillis = Math.max(evaluationTime, System.currentTimeMillis() + 1000L);
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                buildPendingIntent(context, decisionId, title)
        );
        Log.d(TAG, "Scheduled reminder for decision " + decisionId + " at " + triggerAtMillis);
    }

    public static void cancelReminder(Context context, long decisionId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildPendingIntent(context, decisionId, ""));
    }

    private static PendingIntent buildPendingIntent(Context context, long decisionId, String title) {
        Intent intent = new Intent(context, EvaluationReminderReceiver.class);
        intent.putExtra(EXTRA_DECISION_ID, decisionId);
        intent.putExtra(EXTRA_DECISION_TITLE, title);
        return PendingIntent.getBroadcast(
                context,
                toRequestCode(decisionId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static int toRequestCode(long decisionId) {
        return Math.abs((int) (decisionId ^ (decisionId >>> 32)));
    }
}
