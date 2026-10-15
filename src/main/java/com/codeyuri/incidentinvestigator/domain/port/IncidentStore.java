package com.codeyuri.incidentinvestigator.domain.port;

import com.codeyuri.incidentinvestigator.domain.model.Incident;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/** Persistence port for the Incident aggregate. */
public interface IncidentStore {
    Incident save(Incident incident);

    Optional<Incident> findById(UUID id);

    default List<Incident> findByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return List.of();
    }
}
