package com.codeyuri.incidentinvestigator.application.investigation;

public record InvestigationBudget(int maxCalls, int maxTokens) {
    public InvestigationBudget { if (maxCalls < 1 || maxTokens < 1) throw new IllegalArgumentException("Investigation limits must be positive"); }
}
