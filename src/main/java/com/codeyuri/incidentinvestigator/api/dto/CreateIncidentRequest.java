package com.codeyuri.incidentinvestigator.api.dto;

import com.codeyuri.incidentinvestigator.domain.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(min = 5, max = 180) String title,
        @NotBlank @Size(min = 20, max = 50_000) String stackTrace,
        @NotBlank @Size(min = 2, max = 100) String source,
        @NotNull Severity severity
) {
}
