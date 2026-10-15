package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter;

import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository.AnalysisRepository;

import com.codeyuri.incidentinvestigator.domain.model.Analysis;
import com.codeyuri.incidentinvestigator.domain.port.AnalysisStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
class JpaAnalysisStore implements AnalysisStore {
    private final AnalysisRepository repository;

    JpaAnalysisStore(AnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public Analysis save(Analysis analysis) {
        return repository.save(analysis);
    }

    @Override
    public Optional<Analysis> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Analysis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return repository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
}
