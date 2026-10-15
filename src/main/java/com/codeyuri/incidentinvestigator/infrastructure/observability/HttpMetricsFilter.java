package com.codeyuri.incidentinvestigator.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Emits only bounded tags; request bodies, tenant IDs, prompts and stack traces are never metric tags. */
@Component
public class HttpMetricsFilter extends OncePerRequestFilter {
    private final MeterRegistry registry;

    public HttpMetricsFilter(MeterRegistry registry) { this.registry = registry; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Timer.Sample sample = Timer.start(registry);
        try {
            chain.doFilter(request, response);
        } finally {
            String outcome = response.getStatus() >= 500 ? "error" : response.getStatus() >= 400 ? "rejected" : "success";
            String operation = operation(request.getRequestURI());
            sample.stop(Timer.builder("incident_investigator.http.latency")
                    .tag("operation", operation).tag("outcome", outcome).register(registry));
            Counter.builder("incident_investigator.http.requests")
                    .tag("operation", operation).tag("outcome", outcome).register(registry).increment();
        }
    }

    private String operation(String uri) {
        return uri.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}", "{id}")
                .replaceAll("/actuator/[^/]+", "/actuator/{endpoint}");
    }
}
