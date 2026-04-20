package com.sai.decisiongraveyard.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ActivityScheduler {

    private static final String WORK_NAME = "activity_missed_check";
    private static final long CHECK_INTERVAL_MINUTES = 15;

    public static void scheduleMissedActivityCheck(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ActivityMissedWorker.class,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );

        android.util.Log.d("ActivityScheduler", "Scheduled missed activity check every " + CHECK_INTERVAL_MINUTES + " minutes");
    }

    public static void cancelMissedActivityCheck(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        android.util.Log.d("ActivityScheduler", "Cancelled missed activity check");
    }
}
