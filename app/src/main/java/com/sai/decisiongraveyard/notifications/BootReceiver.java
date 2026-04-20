package com.sai.decisiongraveyard.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.repository.DecisionRepository;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        new Thread(() -> {
            try {
                DecisionRepository repository = com.sai.decisiongraveyard.repository.RepositoryProvider.getInstance(context).getDecisionRepository();
                List<Decision> pendingDecisions = repository.getPendingDecisions();
                for (Decision decision : pendingDecisions) {
                    NotificationScheduler.scheduleReminder(
                            context,
                            decision.getId(),
                            decision.getTitle(),
                            decision.getEvaluationTime()
                    );
                }
            } catch (RuntimeException ignored) {
            }
        }).start();
    }
}
