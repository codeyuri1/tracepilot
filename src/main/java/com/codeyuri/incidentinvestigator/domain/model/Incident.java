package com.codeyuri.incidentinvestigator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "stack_trace", nullable = false, columnDefinition = "text")
    private String stackTrace;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "tenant_id", nullable = false, length = 100, updatable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Incident() {
        // Required by JPA.
    }

    private Incident(String title, String stackTrace, String source, Severity severity, String tenantId) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.stackTrace = stackTrace;
        this.source = source;
        this.tenantId = tenantId;
        this.severity = severity;
        this.status = IncidentStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    public static Incident receive(String title, String stackTrace, String source, Severity severity) {
        return receive(title, stackTrace, source, severity, "public");
    }

    public static Incident receive(String title, String stackTrace, String source, Severity severity, String tenantId) {
        if (tenantId == null || tenantId.isBlank() || tenantId.length() > 100) {
            throw new IllegalArgumentException("Tenant must be non-blank and no longer than 100 characters");
        }
        return new Incident(title, stackTrace, source, severity, tenantId);
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStackTrace() { return stackTrace; }
    public String getSource() { return source; }
    public String getTenantId() { return tenantId; }
    public Severity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
