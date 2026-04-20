package com.sai.decisiongraveyard.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sai.decisiongraveyard.R;
import com.sai.decisiongraveyard.model.Activity;
import com.sai.decisiongraveyard.repository.ActivityRepository;
import com.sai.decisiongraveyard.repository.UserProfileRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActivityMissedWorker extends Worker {

    private static final String CHANNEL_ID = "activity_missed_channel";
    private static final String TAG = "ActivityMissedWorker";

    public ActivityMissedWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        android.util.Log.d(TAG, "Checking for missed activities");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            android.util.Log.w(TAG, "No authenticated user, skipping check");
            return Result.success();
        }

        try {
            ActivityRepository repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(getApplicationContext()).getActivityRepository();
            List<Activity> missedActivities = repository.getMissedActivities();

            if (!missedActivities.isEmpty()) {
                android.util.Log.d(TAG, "Found " + missedActivities.size() + " missed activities");
                sendMissedActivityNotification(missedActivities);

                // Award XP penalties for missed activities
                UserProfileRepository profileRepo = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(getApplicationContext()).getUserProfileRepository();
                for (Activity activity : missedActivities) {
                    // Only penalize if not already penalized (track this in a real app)
                    profileRepo.addMissedActivity(new UserProfileRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e(TAG, "Error adding missed activity penalty: " + error);
                        }
                    });
                }

                repository.detectFailurePattern();
            } else {
                android.util.Log.d(TAG, "No missed activities found");
            }

            return Result.success();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking missed activities: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    private void sendMissedActivityNotification(List<Activity> missedActivities) {
        Context context = getApplicationContext();
        createNotificationChannel(context);

        String title;
        String message;

        if (missedActivities.size() == 1) {
            Activity activity = missedActivities.get(0);
            title = "You still have time to fix this";
            message = "Don't let this define you. Complete it now: " + activity.getTitle();
        } else {
            title = "You still have time to fix this";
            message = "You missed " + missedActivities.size() + " activities. Complete one right now to regain control.";
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        android.util.Log.d(TAG, "Notification sent: " + title + " - " + message);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Activity Missed Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for missed activities");
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
