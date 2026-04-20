package com.sai.decisiongraveyard.util;

import android.content.Context;
import android.net.Uri;

import com.sai.decisiongraveyard.model.Decision;
import com.sai.decisiongraveyard.model.DecisionRecord;
import com.sai.decisiongraveyard.model.Evaluation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportUtils {

    private ExportUtils() {
    }

    public static void exportDecisionHistory(Context context, Uri uri, List<DecisionRecord> records)
            throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Title,Description,Category,Decision Time,Evaluation Time,Outcome,Reflection,Evaluated At\n");
        for (DecisionRecord record : records) {
            Decision decision = record.getDecision();
            Evaluation evaluation = record.getEvaluation();
            builder.append(DateUtils.toCsvValue(decision.getTitle())).append(',')
                    .append(DateUtils.toCsvValue(decision.getDescription())).append(',')
                    .append(DateUtils.toCsvValue(DateUtils.getCategoryDisplayName(decision.getCategory()))).append(',')
                    .append(DateUtils.toCsvValue(DateUtils.formatDateTime(decision.getDecisionTime()))).append(',')
                    .append(DateUtils.toCsvValue(DateUtils.formatDateTime(decision.getEvaluationTime()))).append(',')
                    .append(DateUtils.toCsvValue(record.getOutcomeOrPending())).append(',')
                    .append(DateUtils.toCsvValue(evaluation != null ? evaluation.getReflectionNotes() : "")).append(',')
                    .append(DateUtils.toCsvValue(evaluation != null
                            ? DateUtils.formatDateTime(evaluation.getEvaluatedAt()) : ""))
                    .append('\n');
        }

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Unable to open export destination");
            }
            outputStream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
