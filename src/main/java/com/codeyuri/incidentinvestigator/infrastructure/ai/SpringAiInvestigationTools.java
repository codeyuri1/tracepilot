package com.codeyuri.incidentinvestigator.infrastructure.ai;

import com.codeyuri.incidentinvestigator.application.investigation.InvestigationOrchestrator;
import com.codeyuri.incidentinvestigator.application.investigation.ToolCall;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The only Spring AI-facing tool surface. Typed parameters prevent model-supplied extra
 * fields; the stateful orchestrator still enforces allowlist, timeout, cache and budget.
 */
public final class SpringAiInvestigationTools {
    private final InvestigationOrchestrator orchestrator;
    private final String correlationId;

    public SpringAiInvestigationTools(InvestigationOrchestrator orchestrator, String correlationId) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
        if (correlationId == null || correlationId.isBlank()) throw new IllegalArgumentException("Correlation id is required");
        this.correlationId = correlationId;
    }

    @Tool(name = "searchSimilarIncidents", description = "Searches authorized indexed incidents by a technical query.")
    public Map<String, Object> searchSimilarIncidents(
            @ToolParam(description = "Technical search query", required = true) String query,
            @ToolParam(description = "Number of results, from 1 to 10", required = true) int topK) {
        return execute("searchSimilarIncidents", Map.of("query", query, "topK", topK));
    }

    @Tool(name = "searchDocumentation", description = "Searches authorized indexed documentation for one product.")
    public Map<String, Object> searchDocumentation(
            @ToolParam(description = "Technical search query", required = true) String query,
            @ToolParam(description = "Product source namespace", required = true) String product) {
        return execute("searchDocumentation", Map.of("query", query, "product", product));
    }

    @Tool(name = "inspectStackTrace", description = "Returns normalized frames for the current authorized incident only.")
    public Map<String, Object> inspectStackTrace(
            @ToolParam(description = "Current incident UUID", required = true) String incidentId) {
        return execute("inspectStackTrace", Map.of("incidentId", incidentId));
    }

    private Map<String, Object> execute(String name, Map<String, Object> parameters) {
        return orchestrator.execute(new ToolCall(name, parameters), correlationId);
    }
}
