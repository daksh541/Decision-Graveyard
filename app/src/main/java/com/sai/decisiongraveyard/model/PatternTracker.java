package com.sai.decisiongraveyard.model;

import java.util.ArrayList;
import java.util.List;

public class PatternTracker {

    private final List<String> consecutiveMisses;
    private final int totalMisses;
    private final String mostMissedCategory;
    private final boolean isPatternDetected;
    private final String patternMessage;
    private final int recoveryActivitiesNeeded;

    public PatternTracker(List<String> consecutiveMisses, int totalMisses, String mostMissedCategory,
                          boolean isPatternDetected, String patternMessage, int recoveryActivitiesNeeded) {
        this.consecutiveMisses = consecutiveMisses != null ? consecutiveMisses : new ArrayList<>();
        this.totalMisses = totalMisses;
        this.mostMissedCategory = mostMissedCategory;
        this.isPatternDetected = isPatternDetected;
        this.patternMessage = patternMessage;
        this.recoveryActivitiesNeeded = recoveryActivitiesNeeded;
    }

    public List<String> getConsecutiveMisses() {
        return consecutiveMisses;
    }

    public int getTotalMisses() {
        return totalMisses;
    }

    public String getMostMissedCategory() {
        return mostMissedCategory;
    }

    public boolean isPatternDetected() {
        return isPatternDetected;
    }

    public String getPatternMessage() {
        return patternMessage;
    }

    public int getRecoveryActivitiesNeeded() {
        return recoveryActivitiesNeeded;
    }

    public static class Builder {
        private List<String> consecutiveMisses = new ArrayList<>();
        private int totalMisses = 0;
        private String mostMissedCategory = "";
        private boolean isPatternDetected = false;
        private String patternMessage = "";
        private int recoveryActivitiesNeeded = 0;

        public Builder setConsecutiveMisses(List<String> consecutiveMisses) {
            this.consecutiveMisses = consecutiveMisses;
            return this;
        }

        public Builder setTotalMisses(int totalMisses) {
            this.totalMisses = totalMisses;
            return this;
        }

        public Builder setMostMissedCategory(String mostMissedCategory) {
            this.mostMissedCategory = mostMissedCategory;
            return this;
        }

        public Builder setPatternDetected(boolean isPatternDetected) {
            this.isPatternDetected = isPatternDetected;
            return this;
        }

        public Builder setPatternMessage(String patternMessage) {
            this.patternMessage = patternMessage;
            return this;
        }

        public Builder setRecoveryActivitiesNeeded(int recoveryActivitiesNeeded) {
            this.recoveryActivitiesNeeded = recoveryActivitiesNeeded;
            return this;
        }

        public PatternTracker build() {
            return new PatternTracker(consecutiveMisses, totalMisses, mostMissedCategory,
                    isPatternDetected, patternMessage, recoveryActivitiesNeeded);
        }
    }
}
