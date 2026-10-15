package com.codeyuri.incidentinvestigator.api.dto;

import com.codeyuri.incidentinvestigator.domain.model.AnalysisEvidence;

public record AnalysisEvidenceResponse(String type, String reference, String excerpt) {
    static AnalysisEvidenceResponse from(AnalysisEvidence evidence) {
        return new AnalysisEvidenceResponse(evidence.getType(), evidence.getReference(), evidence.getExcerpt());
    }
}
