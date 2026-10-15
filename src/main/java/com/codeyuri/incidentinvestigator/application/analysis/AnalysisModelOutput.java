package com.codeyuri.incidentinvestigator.application.analysis;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisModelOutput(
        String category,
        String summary,
        String probableCause,
        BigDecimal confidence,
        List<AnalysisEvidenceCandidate> evidence,
        List<String> recommendations
) {
}
