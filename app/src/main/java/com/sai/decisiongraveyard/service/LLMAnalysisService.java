package com.sai.decisiongraveyard.service;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LLMAnalysisService {

    private static final String TAG = "LLMAnalysisService";
    
    // You can configure your LLM API endpoint and key here
    // For example, OpenAI API or any other LLM provider
    private static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "YOUR_API_KEY_HERE"; // User should replace this
    
    public String analyzeDecisions(String decisionData) {
        try {
            // For now, return a mock analysis
            // In production, this would call an actual LLM API
            return generateMockAnalysis(decisionData);
            
            // To use real LLM API, uncomment the code below and configure API_KEY
            /*
            String prompt = buildPrompt(decisionData);
            return callLLMAPI(prompt);
            */
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing decisions", e);
            return "Unable to generate analysis at this time. Please try again later.";
        }
    }
    
    private String buildPrompt(String decisionData) {
        return decisionData;
    }
    
    private String callLLMAPI(String prompt) throws Exception {
        URL url = new URL(API_ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        
        String jsonInputString = String.format(
            "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 500}",
            prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API returned error code: " + responseCode);
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        br.close();
        
        // Parse the response to extract the actual content
        // This is a simplified parsing - in production, use a proper JSON parser
        String responseStr = response.toString();
        int contentStart = responseStr.indexOf("\"content\":\"") + 11;
        int contentEnd = responseStr.indexOf("\"", contentStart);
        if (contentStart > 10 && contentEnd > contentStart) {
            return responseStr.substring(contentStart, contentEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
        }
        
        return responseStr;
    }
    
    private String generateMockAnalysis(String decisionData) {
        // This is a mock analysis for demonstration
        // In production, replace this with actual LLM API calls
        
        return "🎯 **Your Decision Analysis**\n\n" +
               "Based on your decision records, here are my insights:\n\n" +
               "**Overall Assessment**\n" +
               "You're showing good awareness of your decision patterns. The fact that you're tracking and reviewing decisions demonstrates self-reflection, which is the first step toward improvement.\n\n" +
               "**Strengths**\n" +
               "• You're actively evaluating your decisions\n" +
               "• You're tracking different categories of decisions\n" +
               "• You're building a habit of self-awareness\n\n" +
               "**Areas for Growth**\n" +
               "• Consider adding more context to your decisions (what was the situation?)\n" +
               "• Try to identify triggers for bad decisions\n" +
               "• Celebrate good decisions to reinforce positive patterns\n\n" +
               "**Actionable Advice**\n" +
               "1. Before making important decisions, pause for 10 seconds\n" +
               "2. Keep a simple decision journal: Situation → Decision → Outcome\n" +
               "3. Review your patterns weekly to identify trends\n" +
               "4. For recurring bad decisions, create a pre-commitment strategy\n\n" +
               "**Encouragement**\n" +
               "Remember: The goal isn't perfection, it's progress. Every decision you track is a step toward better self-awareness and improved decision-making. Keep going! 💪\n\n" +
               "---\n" +
               "*Note: This is a demo analysis. Configure your LLM API key in LLMAnalysisService.java to get personalized AI-powered insights.*";
    }
}
