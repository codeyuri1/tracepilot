package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.DemoKnowledgeRequest;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeDocument;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeIngestionService;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@Profile({"demo", "showcase"})
@RequestMapping("/api/v1/demo/knowledge")
public class DemoKnowledgeController {
    private final KnowledgeIngestionService ingestion;
    private final KnowledgeSearch search;
    public DemoKnowledgeController(KnowledgeIngestionService ingestion, KnowledgeSearch search) { this.ingestion = ingestion; this.search = search; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> ingest(@Valid @RequestBody DemoKnowledgeRequest request) {
        boolean inserted = ingestion.ingest(new KnowledgeDocument(request.source(), request.version(), request.content(),
                Set.of(TenantAccess.currentTenant())));
        return ResponseEntity.status(inserted ? 201 : 200).body(Map.of("inserted", inserted, "source", new com.codeyuri.incidentinvestigator.application.analysis.AuditRedactor().redact(request.source())));
    }

    @GetMapping
    public java.util.List<Map<String, Object>> search(@RequestParam String query) {
        return search.retrieve(query, Set.of(TenantAccess.currentTenant()), 5, 0.01d).stream()
                .map(result -> Map.<String, Object>of("source", result.source(), "excerpt", result.excerpt(), "score", result.score()))
                .toList();
    }
}
