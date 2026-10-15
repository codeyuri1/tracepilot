package com.codeyuri.incidentinvestigator.api.dto;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.IncidentStatus;
import com.codeyuri.incidentinvestigator.domain.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String source,
        Severity severity,
        IncidentStatus status,
        Instant createdAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(), incident.getTitle(), incident.getSource(),
                incident.getSeverity(), incident.getStatus(), incident.getCreatedAt());
    }
}
