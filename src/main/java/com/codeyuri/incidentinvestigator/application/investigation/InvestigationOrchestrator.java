package com.codeyuri.incidentinvestigator.application.investigation;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Stateful per-investigation executor; cache prevents duplicate tool effects in one cycle. */
public final class InvestigationOrchestrator {
    private static final Duration DEFAULT_TOOL_TIMEOUT = Duration.ofSeconds(5);

    private final ToolRegistry registry; private final InvestigationBudget budget; private final Duration toolTimeout;
    private final MeterRegistry meterRegistry;
    private final Map<String, Map<String, Object>> cache = new LinkedHashMap<>();
    private final List<ToolInvocation> audit = new ArrayList<>(); private int usedTokens;

    public InvestigationOrchestrator(ToolRegistry registry, InvestigationBudget budget) {
        this(registry, budget, DEFAULT_TOOL_TIMEOUT);
    }

    public InvestigationOrchestrator(ToolRegistry registry, InvestigationBudget budget, Duration toolTimeout) {
        this(registry, budget, toolTimeout, null);
    }

    public InvestigationOrchestrator(ToolRegistry registry, InvestigationBudget budget, Duration toolTimeout,
                                    MeterRegistry meterRegistry) {
        this.registry = Objects.requireNonNull(registry);
        this.budget = Objects.requireNonNull(budget);
        if (toolTimeout == null || toolTimeout.isZero() || toolTimeout.isNegative()) {
            throw new IllegalArgumentException("Tool timeout must be positive");
        }
        this.toolTimeout = toolTimeout;
        this.meterRegistry = meterRegistry;
    }

    public Map<String,Object> execute(ToolCall call, String correlationId) {
        if (call == null || call.name() == null || call.name().isBlank() || call.parameters() == null) {
            throw new IllegalArgumentException("Tool call must include a name and parameters");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation id is required");
        }
        String key = call.name() + ":" + canonicalParameters(call.parameters());
        Instant started = Instant.now();
        if (cache.containsKey(key)) { audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), true, "CACHED")); return cache.get(key); }
        if (cache.size() >= budget.maxCalls()) {
            audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), false, "CALL_BUDGET_EXHAUSTED"));
            throw new IllegalStateException("Tool call budget exhausted");
        }
        java.util.concurrent.CompletableFuture<Map<String, Object>> invocation = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> registry.resolve(call).execute(call.parameters(), correlationId),
                        new org.springframework.security.concurrent.DelegatingSecurityContextExecutor(
                                java.util.concurrent.ForkJoinPool.commonPool()));
        try {
            Map<String,Object> result = invocation.get(toolTimeout.toNanos(), TimeUnit.NANOSECONDS);
            cache.put(key, result);
            audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), false, "COMPLETED"));
            metric(call.name(), "completed", started);
            return result;
        } catch (TimeoutException exception) {
            invocation.cancel(true);
            audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), false, "TIMEOUT"));
            metric(call.name(), "timeout", started);
            throw new IllegalStateException("Tool call timed out", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), false, "INTERRUPTED"));
            metric(call.name(), "interrupted", started);
            throw new IllegalStateException("Tool call interrupted", exception);
        } catch (ExecutionException exception) {
            audit.add(new ToolInvocation(correlationId, call.name(), key, started, Instant.now(), false, "FAILED"));
            metric(call.name(), "failed", started);
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Tool call failed", exception.getCause());
        }
    }
    public void consumeTokens(int tokens) {
        if (tokens < 0 || usedTokens + tokens > budget.maxTokens()) {
            throw new IllegalStateException("Token budget exhausted");
        }
        usedTokens += tokens;
    }
    public List<ToolInvocation> audit() { return List.copyOf(audit); }

    private static String canonicalParameters(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
    }
    private void metric(String tool, String outcome, Instant started) {
        if (meterRegistry != null) Timer.builder("incident_investigator.tool.latency")
                .tag("tool", tool).tag("outcome", outcome).register(meterRegistry)
                .record(Duration.between(started, Instant.now()));
    }
}
