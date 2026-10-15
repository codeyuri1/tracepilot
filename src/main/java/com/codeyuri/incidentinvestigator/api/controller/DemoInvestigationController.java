package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.DemoInvestigationRequest;
import com.codeyuri.incidentinvestigator.application.investigation.InspectStackTraceTool;
import com.codeyuri.incidentinvestigator.application.investigation.InvestigationBudget;
import com.codeyuri.incidentinvestigator.application.investigation.InvestigationOrchestrator;
import com.codeyuri.incidentinvestigator.application.investigation.SearchDocumentationTool;
import com.codeyuri.incidentinvestigator.application.investigation.SearchSimilarIncidentsTool;
import com.codeyuri.incidentinvestigator.application.investigation.ToolCall;
import com.codeyuri.incidentinvestigator.application.investigation.ToolRegistry;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@Profile({"demo", "showcase"})
@RequestMapping("/api/v1/demo/investigations")
public class DemoInvestigationController {
    private final IncidentStore incidents; private final KnowledgeSearch knowledge;
    public DemoInvestigationController(IncidentStore incidents, KnowledgeSearch knowledge) { this.incidents = incidents; this.knowledge = knowledge; }

    @PostMapping("/{incidentId}")
    public Map<String, Object> execute(@PathVariable UUID incidentId, @Valid @RequestBody DemoInvestigationRequest request) {
        var tools = Set.of(new InspectStackTraceTool(incidents), new SearchDocumentationTool(knowledge, Set.of(TenantAccess.currentTenant())),
                new SearchSimilarIncidentsTool(knowledge, Set.of(TenantAccess.currentTenant())));
        var orchestrator = new InvestigationOrchestrator(new ToolRegistry(tools), new InvestigationBudget(4, 1_000));
        Map<String, Object> parameters = switch (request.tool()) {
            case "inspectStackTrace" -> Map.of("incidentId", incidentId.toString());
            case "searchDocumentation" -> Map.of("query", required(request.query()), "product", required(request.product()));
            case "searchSimilarIncidents" -> Map.of("query", required(request.query()), "topK", request.topK() == null ? 3 : request.topK());
            default -> throw new IllegalArgumentException("Tool is not allowed");
        };
        String correlationId = MDC.get("correlationId");
        Map<String, Object> result = orchestrator.execute(new ToolCall(request.tool(), parameters),
                correlationId == null ? UUID.randomUUID().toString() : correlationId);
        if (Boolean.TRUE.equals(request.repeat())) orchestrator.execute(new ToolCall(request.tool(), parameters),
                correlationId == null ? UUID.randomUUID().toString() : correlationId);
        return Map.of("tool", request.tool(), "result", result, "audit", orchestrator.audit().stream()
                .map(audit -> Map.of("outcome", audit.outcome(), "cached", audit.cached())).toList());
    }
    private String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Required tool input is blank"); return value; }
}
