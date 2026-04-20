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

import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.model.UserPreferences;
import com.sai.decisiongraveyard.repository.UserPreferencesRepository;

import java.util.concurrent.ThreadLocalRandom;

public class PreMissReminderScheduler {

    private static final String CHANNEL_ID = "pre_miss_reminder_channel";
    private static final String TAG = "PreMissReminderScheduler";

    public static void schedulePreMissReminder(Context context, Activity activity) {
        schedulePreMissReminder(context, activity, 10); // Default 10 minutes
    }

    public static void schedulePreMissReminder(Context context, Activity activity, int minutesBefore) {
        schedulePreMissReminder(context, activity, minutesBefore, false);
    }

    public static void schedulePreMissReminder(Context context, Activity activity, int minutesBefore, boolean isBrutal) {
        long scheduledTime = activity.getScheduledTime();
        long currentTime = System.currentTimeMillis();
        long reminderTime = scheduledTime - (minutesBefore * 60 * 1000);

        if (reminderTime <= currentTime) {
            Log.d(TAG, "Reminder time has already passed for activity: " + activity.getTitle());
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PreMissReminderReceiver.class);
        intent.putExtra("activityId", activity.getActivityId());
        intent.putExtra("activityTitle", activity.getTitle());
        intent.putExtra("activityCategory", activity.getCategory());
        intent.putExtra("isBrutal", isBrutal);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) activity.getActivityId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Check for SCHEDULE_EXACT_ALARM permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted. Using inexact alarm.");
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }

        Log.d(TAG, "Scheduled pre-miss reminder for: " + activity.getTitle() + 
                " at: " + new java.util.Date(reminderTime) + " (brutal: " + isBrutal + ")");
    }

    public static void cancelPreMissReminder(Context context, long activityId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PreMissReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) activityId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        android.util.Log.d(TAG, "Cancelled pre-miss reminder for activityId: " + activityId);
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pre-Miss Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders before you miss scheduled activities");
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showPreMissNotification(Context context, String activityTitle, String activityCategory) {
        showPreMissNotification(context, activityTitle, activityCategory, false);
    }

    public static void showPreMissNotification(Context context, String activityTitle, String activityCategory, boolean isBrutal) {
        createNotificationChannel(context);

        String title;
        String message;

        if (isBrutal) {
            title = "You planned this.";
            message = "Don't ignore it now. This is your chance to break the pattern.";
        } else {
            title = "Don't miss this";
            message = "You planned this. Don't ignore it now.";
        }

        Intent intent = new Intent(context, com.sai.decisiongraveyard.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                ThreadLocalRandom.current().nextInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\n" + activityTitle + " (" + 
                                activityCategory.substring(0, 1).toUpperCase() + 
                                activityCategory.substring(1) + ")"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        android.util.Log.d(TAG, "Pre-miss notification shown: " + activityTitle);
    }
}
