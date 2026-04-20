package com.sai.decisiongraveyard.model;

public class Evaluation {

    private long id;
    private long decisionId;
    private String outcome;
    private String reflectionNotes;
    private long evaluatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(long decisionId) {
        this.decisionId = decisionId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getReflectionNotes() {
        return reflectionNotes;
    }

    public void setReflectionNotes(String reflectionNotes) {
        this.reflectionNotes = reflectionNotes;
    }

    public long getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(long evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
