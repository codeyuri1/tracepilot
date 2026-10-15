package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.IncidentResponse;
import com.codeyuri.incidentinvestigator.api.dto.LogEventRequest;
import com.codeyuri.incidentinvestigator.application.intake.LogEvent;
import com.codeyuri.incidentinvestigator.application.intake.LogEventIngestionService;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"showcase", "llm"})
@RequestMapping("/api/v1/demo/log-events")
public class DemoLogIngestionController {
    private final LogEventIngestionService service;
    public DemoLogIngestionController(LogEventIngestionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<IncidentResponse> ingest(@Valid @RequestBody LogEventRequest request) {
        var incident = service.ingest(new LogEvent(request.service(), request.level(), request.message(), request.stackTrace(),
                TenantAccess.currentTenant()));
        return ResponseEntity.status(201).body(IncidentResponse.from(incident));
    }
}
