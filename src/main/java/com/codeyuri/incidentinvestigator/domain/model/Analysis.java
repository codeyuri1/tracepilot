package com.codeyuri.incidentinvestigator.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnalysisStatus status;

    @Column(length = 120)
    private String category;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "probable_cause", columnDefinition = "text")
    private String probableCause;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(columnDefinition = "text")
    private String recommendations;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 32)
    private String promptVersion;

    @Column(name = "prompt_audit", nullable = false, columnDefinition = "text")
    private String promptAudit;

    @Column(name = "response_audit", columnDefinition = "text")
    private String responseAudit;

    @Column(name = "failure_reason", length = 120)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisEvidence> evidence = new ArrayList<>();

    protected Analysis() {
        // Required by JPA.
    }

    private Analysis(Incident incident, String model, String promptVersion, String promptAudit) {
        this.id = UUID.randomUUID();
        this.incident = incident;
        this.status = AnalysisStatus.ANALYZING;
        this.model = model;
        this.promptVersion = promptVersion;
        this.promptAudit = promptAudit;
        this.createdAt = Instant.now();
    }

    public static Analysis start(Incident incident, String model, String promptVersion, String promptAudit) {
        return new Analysis(incident, model, promptVersion, promptAudit);
    }

    public void complete(String category, String summary, String probableCause, BigDecimal confidence,
                         List<String> recommendations, List<AnalysisEvidenceData> evidenceData, String responseAudit) {
        this.category = category;
        this.summary = summary;
        this.probableCause = probableCause;
        this.confidence = confidence;
        this.recommendations = String.join("\n", recommendations);
        this.responseAudit = responseAudit;
        this.evidence.clear();
        evidenceData.forEach(item -> evidence.add(new AnalysisEvidence(this, item.type(), item.reference(), item.excerpt())));
        this.status = AnalysisStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String failureReason, String responseAudit) {
        this.status = AnalysisStatus.FAILED;
        this.failureReason = failureReason;
        this.responseAudit = responseAudit;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AnalysisStatus getStatus() { return status; }
    public String getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getProbableCause() { return probableCause; }
    public BigDecimal getConfidence() { return confidence; }
    public List<String> getRecommendations() { return recommendations == null ? List.of() : List.of(recommendations.split("\\n")); }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public String getPromptAudit() { return promptAudit; }
    public String getResponseAudit() { return responseAudit; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<AnalysisEvidence> getEvidence() { return List.copyOf(evidence); }

    public record AnalysisEvidenceData(String type, String reference, String excerpt) { }
}
