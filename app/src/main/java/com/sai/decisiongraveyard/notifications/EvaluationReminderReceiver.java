package com.sai.decisiongraveyard.notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.sai.decisiongraveyard.DecisionDetailActivity;
import com.sai.decisiongraveyard.R;

public class EvaluationReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationScheduler.createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        long decisionId = intent.getLongExtra(NotificationScheduler.EXTRA_DECISION_ID, -1L);
        String decisionTitle = intent.getStringExtra(NotificationScheduler.EXTRA_DECISION_TITLE);

        Intent detailIntent = new Intent(context, DecisionDetailActivity.class);
        detailIntent.putExtra(DecisionDetailActivity.EXTRA_DECISION_ID, decisionId);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NotificationScheduler.toRequestCode(decisionId),
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NotificationScheduler.CHANNEL_ID
        )
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(context.getString(R.string.decision_due_notification_title))
                .setContentText(context.getString(
                        R.string.decision_due_notification_body,
                        decisionTitle == null ? "your decision" : decisionTitle
                ))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context)
                .notify(NotificationScheduler.toRequestCode(decisionId), builder.build());
    }
}
