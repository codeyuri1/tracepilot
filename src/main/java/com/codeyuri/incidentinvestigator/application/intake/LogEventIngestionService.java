package com.codeyuri.incidentinvestigator.application.intake;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.application.analysis.AuditRedactor;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.springframework.transaction.annotation.Transactional;

public class LogEventIngestionService {
    private final AuditRedactor redactor = new AuditRedactor();
    private final IncidentStore incidents;

    public LogEventIngestionService(IncidentStore incidents) { this.incidents = incidents; }

    @Transactional
    public Incident ingest(LogEvent event) {
        String title = "%s: %s".formatted(redactor.redact(event.service()), redactor.redact(event.message()));
        return incidents.save(Incident.receive(title.substring(0, Math.min(title.length(), 180)), redactor.redact(event.stackTrace()),
                redactor.redact(event.service()), severity(event.level()), event.tenantId()));
    }

    private Severity severity(String level) {
        return switch (level.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ERROR", "FATAL" -> Severity.HIGH;
            case "WARN", "WARNING" -> Severity.MEDIUM;
            default -> Severity.LOW;
        };
    }
}
