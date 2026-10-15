package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository;

import com.codeyuri.incidentinvestigator.domain.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    List<Analysis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
