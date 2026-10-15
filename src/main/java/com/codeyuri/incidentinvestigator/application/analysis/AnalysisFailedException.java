package com.codeyuri.incidentinvestigator.application.analysis;

public class AnalysisFailedException extends RuntimeException {
    public AnalysisFailedException() {
        super("Analysis could not be completed");
    }
}
