package com.sai.decisiongraveyard.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PreMissReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        long activityId = intent.getLongExtra("activityId", -1);
        String activityTitle = intent.getStringExtra("activityTitle");
        String activityCategory = intent.getStringExtra("activityCategory");
        boolean isBrutal = intent.getBooleanExtra("isBrutal", false);

        if (activityId != -1 && activityTitle != null) {
            PreMissReminderScheduler.showPreMissNotification(context, activityTitle, activityCategory, isBrutal);
        }
    }
}
