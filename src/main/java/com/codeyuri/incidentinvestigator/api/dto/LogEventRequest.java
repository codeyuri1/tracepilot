package com.codeyuri.incidentinvestigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogEventRequest(
        @NotBlank @Size(max = 100) String service,
        @NotBlank @Size(max = 16) String level,
        @NotBlank @Size(max = 500) String message,
        @NotBlank @Size(min = 20, max = 50_000) String stackTrace
) { }
