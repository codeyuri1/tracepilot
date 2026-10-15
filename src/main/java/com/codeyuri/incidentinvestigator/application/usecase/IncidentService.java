package com.codeyuri.incidentinvestigator.application.usecase;

import com.codeyuri.incidentinvestigator.application.exception.IncidentNotFoundException;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.application.analysis.AuditRedactor;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class IncidentService {
    private final AuditRedactor redactor = new AuditRedactor();
    private final IncidentStore incidentStore;

    public IncidentService(IncidentStore incidentStore) {
        this.incidentStore = incidentStore;
    }

    @Transactional
    public Incident create(String title, String stackTrace, String source, Severity severity) {
        return incidentStore.save(Incident.receive(redactor.redact(title), redactor.redact(stackTrace), redactor.redact(source), severity, TenantAccess.currentTenant()));
    }

    @Transactional(readOnly = true)
    public Incident get(UUID id) {
        Incident incident = incidentStore.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (!TenantAccess.currentTenant().equals(incident.getTenantId())) {
            // A cross-tenant lookup is intentionally indistinguishable from an absent ID.
            throw new IncidentNotFoundException(id);
        }
        return incident;
    }

    @Transactional(readOnly = true)
    public List<Incident> list() {
        return incidentStore.findByTenantIdOrderByCreatedAtDesc(TenantAccess.currentTenant());
    }
}
