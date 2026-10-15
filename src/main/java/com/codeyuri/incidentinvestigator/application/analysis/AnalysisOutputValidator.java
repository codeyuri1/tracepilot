package com.codeyuri.incidentinvestigator.application.analysis;

import com.codeyuri.incidentinvestigator.domain.model.Analysis.AnalysisEvidenceData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AnalysisOutputValidator {

    public List<AnalysisEvidenceData> validate(AnalysisModelOutput output, String suppliedMaterial) {
        if (output == null || isBlank(output.category()) || isBlank(output.summary()) || isBlank(output.probableCause())) {
            throw new AnalysisOutputValidationException("Required analysis fields are missing");
        }
        BigDecimal confidence = output.confidence();
        if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new AnalysisOutputValidationException("Confidence must be between zero and one");
        }
        if (output.evidence() == null || output.evidence().isEmpty()) {
            throw new AnalysisOutputValidationException("At least one evidence item is required");
        }
        return output.evidence().stream().map(evidence -> validateEvidence(evidence, suppliedMaterial)).toList();
    }

    private AnalysisEvidenceData validateEvidence(AnalysisEvidenceCandidate evidence, String suppliedMaterial) {
        if (evidence == null || isBlank(evidence.type()) || isBlank(evidence.reference()) || isBlank(evidence.excerpt())) {
            throw new AnalysisOutputValidationException("Evidence must include type, reference, and excerpt");
        }
        if (!suppliedMaterial.contains(evidence.excerpt())) {
            throw new AnalysisOutputValidationException("Evidence excerpt is not present in supplied material");
        }
        return new AnalysisEvidenceData(evidence.type(), evidence.reference(), evidence.excerpt());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
