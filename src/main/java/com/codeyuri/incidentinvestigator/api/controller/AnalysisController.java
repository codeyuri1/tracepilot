package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.AnalysisResponse;

import com.codeyuri.incidentinvestigator.application.usecase.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/analyses")
public class AnalysisController {
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(@PathVariable UUID incidentId) {
        AnalysisResponse analysis = analysisService.analyze(incidentId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{analysisId}")
                .buildAndExpand(analysis.id()).toUri();
        return ResponseEntity.created(location).body(analysis);
    }

    @GetMapping
    public List<AnalysisResponse> history(@PathVariable UUID incidentId) {
        return analysisService.history(incidentId);
    }
}
