package com.codeyuri.incidentinvestigator.application.investigation;

import java.time.Instant;

public record ToolInvocation(String correlationId, String tool, String cacheKey, Instant startedAt, Instant completedAt,
                             boolean cached, String outcome) { }
