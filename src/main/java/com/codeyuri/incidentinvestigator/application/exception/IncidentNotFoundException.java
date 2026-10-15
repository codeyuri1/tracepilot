package com.codeyuri.incidentinvestigator.application.exception;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID id) {
        super("Incident %s was not found".formatted(id));
    }
}
