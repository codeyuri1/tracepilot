package com.codeyuri.incidentinvestigator.application.investigation;

import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InspectStackTraceTool implements InvestigationTool {
    private final IncidentStore incidents;
    public InspectStackTraceTool(IncidentStore incidents) { this.incidents = incidents; }
    public String name() { return "inspectStackTrace"; }
    public Map<String, Object> execute(Map<String, Object> parameters, String correlationId) {
        if (!parameters.keySet().equals(java.util.Set.of("incidentId"))) throw new IllegalArgumentException("Invalid tool parameters");
        UUID id = UUID.fromString(String.valueOf(parameters.get("incidentId")));
        var incident = incidents.findById(id).orElseThrow();
        if (!TenantAccess.currentTenant().equals(incident.getTenantId())) throw new IllegalArgumentException("Incident is not available");
        String stackTrace = incident.getStackTrace();
        List<String> frames = stackTrace.lines().map(String::trim).filter(line -> line.startsWith("at "))
                .map(line -> line.substring(3)).toList();
        return Map.of("incidentId", id.toString(), "frames", frames, "correlationId", correlationId);
    }
}
