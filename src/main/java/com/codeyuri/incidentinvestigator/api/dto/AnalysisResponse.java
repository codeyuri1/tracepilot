package com.codeyuri.incidentinvestigator.api.dto;

import com.codeyuri.incidentinvestigator.domain.model.Analysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        String category,
        String summary,
        String probableCause,
        BigDecimal confidence,
        List<AnalysisEvidenceResponse> evidence,
        List<String> recommendations,
        String model,
        String promptVersion
) {
    public static AnalysisResponse from(Analysis analysis) {
        return new AnalysisResponse(analysis.getId(), analysis.getCategory(), analysis.getSummary(),
                analysis.getProbableCause(), analysis.getConfidence(),
                analysis.getEvidence().stream().map(AnalysisEvidenceResponse::from).toList(),
                analysis.getRecommendations(), analysis.getModel(), analysis.getPromptVersion());
    }
}
