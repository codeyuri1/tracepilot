package com.codeyuri.incidentinvestigator.application.analysis;

public class AnalysisModelTimeoutException extends RuntimeException {
    public AnalysisModelTimeoutException() {
        super("Analysis model timed out");
    }
}
