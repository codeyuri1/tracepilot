package com.codeyuri.incidentinvestigator.application.investigation;

import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;
import com.codeyuri.incidentinvestigator.application.retrieval.RetrievalResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Searches only ACL-authorized, indexed documentation for one product source namespace. */
public final class SearchDocumentationTool implements InvestigationTool {
    private static final int RESULT_LIMIT = 5;
    private static final double SCORE_THRESHOLD = 0.01d;

    private final KnowledgeSearch knowledge;
    private final Set<String> requesterAcl;

    public SearchDocumentationTool(KnowledgeSearch knowledge, Set<String> requesterAcl) {
        this.knowledge = knowledge;
        this.requesterAcl = Set.copyOf(requesterAcl);
    }

    @Override
    public String name() {
        return "searchDocumentation";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters, String correlationId) {
        if (!parameters.keySet().equals(Set.of("query", "product"))) {
            throw new IllegalArgumentException("Invalid tool parameters");
        }
        String query = requiredText(parameters, "query");
        String product = requiredText(parameters, "product");
        List<Map<String, Object>> documents = knowledge.retrieve(query, requesterAcl, RESULT_LIMIT * 4, SCORE_THRESHOLD).stream()
                .filter(result -> belongsToProduct(result, product))
                .limit(RESULT_LIMIT)
                .map(result -> Map.<String, Object>of("source", result.source(), "excerpt", result.excerpt(), "score", result.score()))
                .toList();
        return Map.of("product", product, "documents", documents, "correlationId", correlationId);
    }

    private static boolean belongsToProduct(RetrievalResult result, String product) {
        return result.source().equals(product)
                || result.source().startsWith(product + ":")
                || result.source().startsWith(product + "/");
    }

    private static String requiredText(Map<String, Object> parameters, String name) {
        Object value = parameters.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Parameter %s must be non-blank text".formatted(name));
        }
        return text.trim();
    }
}
