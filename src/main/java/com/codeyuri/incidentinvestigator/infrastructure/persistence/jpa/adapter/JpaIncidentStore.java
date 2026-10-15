package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter;

import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository.IncidentRepository;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
class JpaIncidentStore implements IncidentStore {
    private final IncidentRepository repository;

    JpaIncidentStore(IncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Incident save(Incident incident) {
        return repository.save(incident);
    }

    @Override
    public Optional<Incident> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Incident> findByTenantIdOrderByCreatedAtDesc(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
