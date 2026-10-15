package com.codeyuri.incidentinvestigator.application.intake;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogEventIngestionServiceTest {
    @Test
    void convertsAnErrorLogIntoAHigherSeverityIncident() {
        CapturingStore store = new CapturingStore();
        LogEventIngestionService service = new LogEventIngestionService(store);

        Incident incident = service.ingest(new LogEvent("payment-worker", "ERROR", "PostgreSQL unavailable",
                "org.postgresql.util.PSQLException: Connection refused", "demo"));

        assertThat(incident.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(incident.getSource()).isEqualTo("payment-worker");
        assertThat(store.saved).isSameAs(incident);
    }

    @Test
    void sanitizesBeforeSavingOrReturningIncident() {
        CapturingStore store = new CapturingStore();
        Incident incident = new LogEventIngestionService(store).ingest(new LogEvent(
                "worker", "ERROR", "Connection failed password=synthetic-pass",
                "Connection refused Authorization: Bearer demo-token email=person@example.test", "demo"));
        assertThat(store.saved.getTitle()).doesNotContain("synthetic-pass");
        assertThat(store.saved.getStackTrace()).doesNotContain("demo-token", "person@example.test");
        assertThat(incident.getStackTrace()).contains("Connection refused");
    }

    private static class CapturingStore implements IncidentStore {
        private Incident saved;
        @Override public Incident save(Incident incident) { return saved = incident; }
        @Override public Optional<Incident> findById(UUID id) { return Optional.empty(); }
    }
}
