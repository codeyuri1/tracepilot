package com.codeyuri.incidentinvestigator.application.investigation;

import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Searches incident documents explicitly indexed with the source convention {@code incident:<UUID>}. */
public final class SearchSimilarIncidentsTool implements InvestigationTool {
    private static final int MAX_TOP_K = 10;
    private static final double SCORE_THRESHOLD = 0.01d;

    private final KnowledgeSearch knowledge;
    private final Set<String> requesterAcl;

    public SearchSimilarIncidentsTool(KnowledgeSearch knowledge, Set<String> requesterAcl) {
        this.knowledge = knowledge;
        this.requesterAcl = Set.copyOf(requesterAcl);
    }

    @Override
    public String name() {
        return "searchSimilarIncidents";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters, String correlationId) {
        if (!parameters.keySet().equals(Set.of("query", "topK"))) {
            throw new IllegalArgumentException("Invalid tool parameters");
        }
        String query = requiredQuery(parameters.get("query"));
        int topK = requiredTopK(parameters.get("topK"));
        List<Map<String, Object>> incidents = knowledge.retrieve(query, requesterAcl, MAX_TOP_K * 4, SCORE_THRESHOLD).stream()
                .filter(result -> result.source().startsWith("incident:"))
                .map(result -> Map.entry(incidentId(result.source()), result))
                .filter(entry -> entry.getKey() != null)
                .limit(topK)
                .map(entry -> Map.<String, Object>of("id", entry.getKey(), "score", entry.getValue().score(),
                        "excerpt", entry.getValue().excerpt()))
                .toList();
        return Map.of("incidents", incidents, "correlationId", correlationId);
    }

    private static String incidentId(String source) {
        String value = source.substring("incident:".length());
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String requiredQuery(Object value) {
        if (!(value instanceof String query) || query.isBlank()) {
            throw new IllegalArgumentException("Parameter query must be non-blank text");
        }
        return query.trim();
    }

    private static int requiredTopK(Object value) {
        if (!(value instanceof Number number) || number.intValue() != number.doubleValue()
                || number.intValue() < 1 || number.intValue() > MAX_TOP_K) {
            throw new IllegalArgumentException("Parameter topK must be an integer between 1 and " + MAX_TOP_K);
        }
        return number.intValue();
    }
}
