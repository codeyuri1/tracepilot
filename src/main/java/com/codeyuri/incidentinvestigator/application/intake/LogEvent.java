package com.codeyuri.incidentinvestigator.application.intake;

/** Normalized synthetic or external log event; tenant is obtained by the authenticated caller. */
public record LogEvent(String service, String level, String message, String stackTrace, String tenantId) {
    public LogEvent {
        if (blank(service) || blank(level) || blank(message) || blank(stackTrace) || blank(tenantId)) {
            throw new IllegalArgumentException("Log event fields must be non-blank");
        }
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
