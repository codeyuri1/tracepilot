package com.codeyuri.incidentinvestigator.api;

import com.codeyuri.incidentinvestigator.api.controller.AnalysisController;
import com.codeyuri.incidentinvestigator.api.dto.AnalysisResponse;
import com.codeyuri.incidentinvestigator.api.error.ApiExceptionHandler;
import com.codeyuri.incidentinvestigator.application.usecase.AnalysisService;
import com.codeyuri.incidentinvestigator.domain.model.Analysis;
import com.codeyuri.incidentinvestigator.domain.model.Analysis.AnalysisEvidenceData;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisController(new StubAnalysisService()))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsOnlyThePublicAnalysisContract() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/{incidentId}/analyses", UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.category").value("DATABASE_CONNECTIVITY"))
                .andExpect(jsonPath("$.evidence[0].excerpt").value("Connection refused"))
                .andExpect(jsonPath("$.promptAudit").doesNotExist())
                .andExpect(jsonPath("$.responseAudit").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }

    @Test
    void exposesPersistedAnalysisHistoryWithoutAuditFields() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/{incidentId}/analyses", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("DATABASE_CONNECTIVITY"))
                .andExpect(jsonPath("$[0].promptAudit").doesNotExist())
                .andExpect(jsonPath("$[0].responseAudit").doesNotExist());
    }

    static class StubAnalysisService extends AnalysisService {
        StubAnalysisService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public AnalysisResponse analyze(UUID incidentId) {
            return response();
        }

        @Override
        public List<AnalysisResponse> history(UUID incidentId) {
            return List.of(response());
        }

        private AnalysisResponse response() {
            Incident incident = Incident.receive("Payment worker cannot connect",
                    "org.postgresql.util.PSQLException: Connection refused", "payment-worker", Severity.CRITICAL);
            Analysis analysis = Analysis.start(incident, "deterministic-test-model", "v1", "redacted");
            analysis.complete("DATABASE_CONNECTIVITY", "The worker cannot reach PostgreSQL.", "Connection failed.",
                    new BigDecimal("0.80"), List.of("Check PostgreSQL availability."),
                    List.of(new AnalysisEvidenceData("STACK_TRACE", "incident.stackTrace", "Connection refused")), "redacted");
            return AnalysisResponse.from(analysis);
        }
    }
}
