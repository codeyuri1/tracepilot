package com.codeyuri.incidentinvestigator.application.analysis;

public interface AnalysisModelClient {
    AnalysisModelOutput analyze(AnalysisPrompt prompt);

    String modelName();
}
