package com.sai.decisiongraveyard.service;

import com.sai.decisiongraveyard.model.AiInsightReport;
import com.sai.decisiongraveyard.model.AnalyticsSnapshot;

public class LLMAnalysisService {

    public String buildNarrative(AnalyticsSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }

        AiInsightReport report = snapshot.getAiInsightReport();
        StringBuilder builder = new StringBuilder();
        builder.append(report.getSummary()).append("\n\n");
        builder.append("Behavior score: ").append(report.getBehaviorScore()).append("/100\n");
        builder.append("Discipline score: ").append(report.getDisciplineScore()).append("/100\n");
        builder.append("Risk pressure: ").append(report.getRiskScore()).append("/100\n\n");
        builder.append(report.getWeeklyReport()).append("\n\n");

        for (String insight : report.getRecommendations()) {
            builder.append("• ").append(insight).append('\n');
        }
        return builder.toString().trim();
    }
}
