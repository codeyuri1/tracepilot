package com.codeyuri.incidentinvestigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DemoInvestigationRequest(
        @NotBlank String tool,
        String query,
        String product,
        @Min(1) @Max(10) Integer topK,
        Boolean repeat
) { }
