package com.codeyuri.incidentinvestigator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "analysis_evidence")
public class AnalysisEvidence {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "evidence_type", nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 255)
    private String reference;

    @Column(nullable = false, columnDefinition = "text")
    private String excerpt;

    protected AnalysisEvidence() {
        // Required by JPA.
    }

    AnalysisEvidence(Analysis analysis, String type, String reference, String excerpt) {
        this.id = UUID.randomUUID();
        this.analysis = analysis;
        this.type = type;
        this.reference = reference;
        this.excerpt = excerpt;
    }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public String getReference() { return reference; }
    public String getExcerpt() { return excerpt; }
}
