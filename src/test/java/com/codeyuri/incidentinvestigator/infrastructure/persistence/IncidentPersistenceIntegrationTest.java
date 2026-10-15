package com.codeyuri.incidentinvestigator.infrastructure.persistence;

import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository.IncidentRepository;

import com.codeyuri.incidentinvestigator.application.usecase.IncidentService;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class IncidentPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void directIncidentEntryPersistsOnlySanitizedMaterial() {
        Incident created = incidentService.create("Connection refused password=synthetic-direct-secret",
                "Connection refused Authorization: Basic ZGVtbzpwYXNz contact person@example.test", "payment-worker", Severity.HIGH);
        Incident persisted = incidentRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getTitle()).doesNotContain("synthetic-direct-secret");
        assertThat(persisted.getStackTrace()).contains("Connection refused", "[REDACTED]")
                .doesNotContain("ZGVtbzpwYXNz", "person@example.test");
    }

    @Test
    void keepsAnIncidentAfterTheCreateTransactionCompletes() {
        Incident created = incidentService.create(
                "Payment worker cannot connect", "org.postgresql.util.PSQLException: Connection refused",
                "payment-worker", Severity.CRITICAL);

        Incident persisted = incidentRepository.findById(created.getId()).orElseThrow();

        assertThat(persisted.getTitle()).isEqualTo("Payment worker cannot connect");
        assertThat(persisted.getStackTrace()).isEqualTo("org.postgresql.util.PSQLException: Connection refused");
        assertThat(persisted.getSource()).isEqualTo("payment-worker");
        assertThat(persisted.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(persisted.getStatus()).hasToString("RECEIVED");
        assertThat(persisted.getCreatedAt()).isNotNull();
    }
}
