package com.sai.decisiongraveyard.model;

public class DecisionRecord {

    private final Decision decision;
    private final Evaluation evaluation;

    public DecisionRecord(Decision decision, Evaluation evaluation) {
        this.decision = decision;
        this.evaluation = evaluation;
    }

    public Decision getDecision() {
        return decision;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public boolean isEvaluated() {
        return evaluation != null;
    }

    public boolean isReadyForReview() {
        return !isEvaluated() && System.currentTimeMillis() >= decision.getEvaluationTime();
    }

    public boolean isUpcoming() {
        return !isEvaluated() && !isReadyForReview();
    }

    public String getOutcomeOrPending() {
        return evaluation != null ? evaluation.getOutcome() : "pending";
    }
}
