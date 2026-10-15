package com.codeyuri.incidentinvestigator.application;

import com.codeyuri.incidentinvestigator.application.analysis.AnalysisEvidenceCandidate;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisFailedException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelClient;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelOutput;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelTimeoutException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPrompt;
import com.codeyuri.incidentinvestigator.application.usecase.AnalysisService;
import com.codeyuri.incidentinvestigator.domain.model.AnalysisStatus;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository.AnalysisRepository;
import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.repository.IncidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Import(AnalysisServiceIntegrationTest.ModelConfiguration.class)
class AnalysisServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private DeterministicAnalysisModelClient modelClient;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @AfterEach
    void cleanDatabase() {
        analysisRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    void convertsDeterministicModelOutputToTheAnalysisContractAndRedactsAudit() {
        Incident incident = incidentRepository.save(Incident.receive(
                "Payment worker cannot connect", "token=top-secret org.postgresql.util.PSQLException: Connection refused",
                "payment-worker", Severity.CRITICAL));
        modelClient.response = prompt -> new AnalysisModelOutput(
                "DATABASE_CONNECTIVITY", "The worker cannot reach PostgreSQL.", "The stack trace reports a refused connection.",
                new BigDecimal("0.80"),
                List.of(new AnalysisEvidenceCandidate("STACK_TRACE", "incident.stackTrace", "Connection refused")),
                List.of("Check PostgreSQL availability."));

        var response = analysisService.analyze(incident.getId());

        assertThat(response.category()).isEqualTo("DATABASE_CONNECTIVITY");
        assertThat(response.confidence()).isEqualByComparingTo("0.80");
        assertThat(response.evidence()).singleElement().satisfies(evidence ->
                assertThat(evidence.excerpt()).isEqualTo("Connection refused"));
        var persisted = analysisRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(persisted.getPromptAudit()).contains("token=[REDACTED]").doesNotContain("top-secret");
    }

    @Test
    void marksAnalysisAsFailedWhenModelOutputHasNoValidEvidence() {
        Incident incident = incidentRepository.save(Incident.receive(
                "Payment worker cannot connect", "org.postgresql.util.PSQLException: Connection refused",
                "payment-worker", Severity.CRITICAL));
        modelClient.response = prompt -> new AnalysisModelOutput(
                "DATABASE_CONNECTIVITY", "The worker cannot reach PostgreSQL.", "Connection failed.",
                new BigDecimal("0.80"), List.of(), List.of("Check PostgreSQL availability."));

        assertThatThrownBy(() -> analysisService.analyze(incident.getId()))
                .isInstanceOf(AnalysisFailedException.class);

        var persisted = analysisRepository.findAll().getFirst();
        assertThat(persisted.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(persisted.getFailureReason()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void marksAnalysisAsFailedWhenModelTimesOut() {
        Incident incident = incidentRepository.save(Incident.receive(
                "Payment worker cannot connect", "org.postgresql.util.PSQLException: Connection refused",
                "payment-worker", Severity.CRITICAL));
        modelClient.response = prompt -> {
            throw new AnalysisModelTimeoutException();
        };

        assertThatThrownBy(() -> analysisService.analyze(incident.getId()))
                .isInstanceOf(AnalysisFailedException.class);

        var persisted = analysisRepository.findAll().getFirst();
        assertThat(persisted.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(persisted.getFailureReason()).isEqualTo("TIMEOUT");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ModelConfiguration {
        @Bean
        DeterministicAnalysisModelClient analysisModelClient() {
            return new DeterministicAnalysisModelClient();
        }
    }

    static class DeterministicAnalysisModelClient implements AnalysisModelClient {
        private Function<AnalysisPrompt, AnalysisModelOutput> response;

        @Override
        public AnalysisModelOutput analyze(AnalysisPrompt prompt) {
            return response.apply(prompt);
        }

        @Override
        public String modelName() {
            return "deterministic-test-model";
        }
    }
}
