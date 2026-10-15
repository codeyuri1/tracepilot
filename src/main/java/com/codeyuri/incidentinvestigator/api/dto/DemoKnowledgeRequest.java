package com.codeyuri.incidentinvestigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemoKnowledgeRequest(
        @NotBlank @Size(max = 255) String source,
        @NotBlank @Size(max = 120) String version,
        @NotBlank @Size(max = 50_000) String content
) { }
