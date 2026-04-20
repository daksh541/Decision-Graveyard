package com.sai.decisiongraveyard.repository;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sai.decisiongraveyard.BuildConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LLMService {
    private static final String TAG = "LLMService";
    private final GenerativeModelFutures model;
    private final java.util.concurrent.Executor executor;

    public LLMService(Context context) {
        this.executor = ContextCompat.getMainExecutor(context);
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "GEMINI_API_KEY not configured. Using fallback to pre-defined texts.");
        }
        GenerativeModel generativeModel = new GenerativeModel(
            "gemini-1.5-flash",
            apiKey
        );
        this.model = GenerativeModelFutures.from(generativeModel);
    }

    public CompletableFuture<String> generateInsights(String decisionData, boolean brutalMode) {
        String promptText = buildInsightPrompt(decisionData, brutalMode);
        Content content = new Content.Builder().addText(promptText).build();
        CompletableFuture<String> future = new CompletableFuture<>();
        
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new com.google.common.util.concurrent.FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                future.complete(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error generating insights: " + t.getMessage(), t);
                future.complete(null);
            }
        }, executor);

        return future;
    }

    public CompletableFuture<String> generateActionableInsights(String decisionData) {
        String promptText = buildActionablePrompt(decisionData);
        Content content = new Content.Builder().addText(promptText).build();
        CompletableFuture<String> future = new CompletableFuture<>();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new com.google.common.util.concurrent.FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                future.complete(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error generating actionable insights: " + t.getMessage(), t);
                future.complete(null);
            }
        }, executor);

        return future;
    }

    public CompletableFuture<String> generateActivityInsights(String activityData) {
        String promptText = buildActivityPrompt(activityData);
        Content content = new Content.Builder().addText(promptText).build();
        CompletableFuture<String> future = new CompletableFuture<>();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new com.google.common.util.concurrent.FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                future.complete(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error generating activity insights: " + t.getMessage(), t);
                future.complete(null);
            }
        }, executor);

        return future;
    }

    private String buildInsightPrompt(String decisionData, boolean brutalMode) {
        String tone = brutalMode 
            ? "Be brutally honest, direct, and harsh. Call out patterns of self-sabotage. Use strong language."
            : "Be analytical, objective, and helpful. Focus on patterns without being overly harsh.";
        
        return String.format(
            "You are analyzing a user's decision-making patterns. %s\n\n" +
            "Decision Data:\n%s\n\n" +
            "Generate 2-3 concise insights (max 20 words each) about their decision patterns. " +
            "Focus on: categories they struggle with, time-based patterns, and overall judgment quality. " +
            "Return as a simple list, each on a new line.",
            tone, decisionData
        );
    }

    private String buildActionablePrompt(String decisionData) {
        return String.format(
            "You are analyzing a user's decision-making patterns.\n\n" +
            "Decision Data:\n%s\n\n" +
            "Generate 2-3 specific, actionable recommendations (max 15 words each) to improve their decision-making. " +
            "Focus on practical changes they can implement immediately. " +
            "Return as a simple list, each on a new line.",
            decisionData
        );
    }

    private String buildActivityPrompt(String activityData) {
        return String.format(
            "You are analyzing a user's activity completion patterns and their connection to decision quality.\n\n" +
            "Activity Data:\n%s\n\n" +
            "Generate 2-3 insights about how missed activities affect their decisions (max 20 words each). " +
            "Focus on cause-effect relationships. " +
            "Return as a simple list, each on a new line.",
            activityData
        );
    }
}
