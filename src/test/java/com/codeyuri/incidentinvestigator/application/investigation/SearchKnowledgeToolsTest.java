package com.codeyuri.incidentinvestigator.application.investigation;

import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;
import com.codeyuri.incidentinvestigator.application.retrieval.RetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchKnowledgeToolsTest {
    private static final String INCIDENT_ID = UUID.randomUUID().toString();
    private final KnowledgeSearch knowledge = (query, acl, topK, threshold) -> List.of(
            new RetrievalResult("1", "payments:runbook", "Retry the payment consumer.", 0.8d),
            new RetrievalResult("2", "orders:runbook", "Restart the order consumer.", 0.7d),
            new RetrievalResult("3", "incident:" + INCIDENT_ID, "Connection refused to PostgreSQL.", 0.9d));

    @Test
    void documentationSearchReturnsOnlyTheRequestedProductNamespace() {
        var tool = new SearchDocumentationTool(knowledge, Set.of("operations"));

        Map<String, Object> result = tool.execute(Map.of("query", "consumer unavailable", "product", "payments"), "corr-1");

        assertThat(result.get("documents")).isEqualTo(List.of(Map.of("source", "payments:runbook",
                "excerpt", "Retry the payment consumer.", "score", 0.8d)));
        assertThat(result.get("correlationId")).isEqualTo("corr-1");
    }

    @Test
    void similarIncidentSearchReturnsOnlyCanonicalIncidentIdsAndRejectsExtraParameters() {
        var tool = new SearchSimilarIncidentsTool(knowledge, Set.of("operations"));

        Map<String, Object> result = tool.execute(Map.of("query", "database unavailable", "topK", 2), "corr-2");

        assertThat(result.get("incidents")).isEqualTo(List.of(Map.of("id", INCIDENT_ID, "score", 0.9d,
                "excerpt", "Connection refused to PostgreSQL.")));
        assertThatThrownBy(() -> tool.execute(Map.of("query", "database", "topK", 1, "acl", "admin"), "corr-2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
