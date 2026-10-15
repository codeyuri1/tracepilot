package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
