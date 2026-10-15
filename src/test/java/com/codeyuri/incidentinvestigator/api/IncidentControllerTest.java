package com.codeyuri.incidentinvestigator.api;

import com.codeyuri.incidentinvestigator.api.controller.*;
import com.codeyuri.incidentinvestigator.api.dto.*;
import com.codeyuri.incidentinvestigator.api.error.ApiExceptionHandler;

import com.codeyuri.incidentinvestigator.application.exception.IncidentNotFoundException;
import com.codeyuri.incidentinvestigator.application.usecase.IncidentService;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentControllerTest {

    private MockMvc mockMvc;

    private StubIncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new StubIncidentService();
        mockMvc = MockMvcBuilders.standaloneSetup(new IncidentController(incidentService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsIncidentAndReturnsPublicFields() throws Exception {
        Incident incident = Incident.receive(
                "Order endpoint fails", "java.lang.IllegalStateException: session is closed",
                "orders-api", Severity.HIGH);
        incidentService.createdIncident = incident;

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Order endpoint fails","stackTrace":"java.lang.IllegalStateException: session is closed","source":"orders-api","severity":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/incidents/" + incident.getId())))
                .andExpect(jsonPath("$.id").value(incident.getId().toString()))
                .andExpect(jsonPath("$.title").value("Order endpoint fails"))
                .andExpect(jsonPath("$.source").value("orders-api"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void returnsProblemDetailsWithFieldViolationsForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"bad","stackTrace":"secret stack trace","source":"x","severity":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.violations[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'stackTrace')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'source')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field == 'severity')]").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void returnsProblemDetailsWhenIncidentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/incidents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Incident not found"))
                .andExpect(jsonPath("$.detail").value("Incident " + id + " was not found"));
    }

    @Test
    void listsOnlyPublicIncidentFieldsForTheCurrentTenant() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Order endpoint fails"))
                .andExpect(jsonPath("$[0].stackTrace").doesNotExist());
    }

    static class StubIncidentService extends IncidentService {
        private Incident createdIncident;

        StubIncidentService() {
            super(null);
        }

        @Override
        public Incident create(String title, String stackTrace, String source, Severity severity) {
            return createdIncident;
        }

        @Override
        public Incident get(UUID id) {
            throw new IncidentNotFoundException(id);
        }

        @Override
        public List<Incident> list() {
            return List.of(Incident.receive("Order endpoint fails", "secret stack trace", "orders-api", Severity.HIGH));
        }
    }
}
