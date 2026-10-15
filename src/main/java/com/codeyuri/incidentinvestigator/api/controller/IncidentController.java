package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.CreateIncidentRequest;
import com.codeyuri.incidentinvestigator.api.dto.IncidentResponse;

import com.codeyuri.incidentinvestigator.application.usecase.IncidentService;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        Incident incident = incidentService.create(
                request.title(), request.stackTrace(), request.source(), request.severity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(incident.getId()).toUri();
        return ResponseEntity.created(location).body(IncidentResponse.from(incident));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable UUID id) {
        return IncidentResponse.from(incidentService.get(id));
    }

    @GetMapping
    public List<IncidentResponse> list() {
        return incidentService.list().stream().map(IncidentResponse::from).toList();
    }
}
