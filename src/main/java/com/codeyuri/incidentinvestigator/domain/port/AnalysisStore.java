package com.codeyuri.incidentinvestigator.domain.port;

import com.codeyuri.incidentinvestigator.domain.model.Analysis;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/** Persistence port for the Analysis aggregate. */
public interface AnalysisStore {
    Analysis save(Analysis analysis);

    Optional<Analysis> findById(UUID id);

    default List<Analysis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return List.of();
    }
}
