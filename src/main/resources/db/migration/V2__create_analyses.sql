CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    status VARCHAR(16) NOT NULL,
    category VARCHAR(120),
    summary TEXT,
    probable_cause TEXT,
    confidence NUMERIC(3, 2),
    recommendations TEXT,
    model VARCHAR(120) NOT NULL,
    prompt_version VARCHAR(32) NOT NULL,
    prompt_audit TEXT NOT NULL,
    response_audit TEXT,
    failure_reason VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_analyses_status CHECK (status IN ('ANALYZING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_analyses_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE TABLE analysis_evidence (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES analyses(id) ON DELETE CASCADE,
    evidence_type VARCHAR(80) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    excerpt TEXT NOT NULL
);

CREATE INDEX idx_analyses_incident_id ON analyses (incident_id);
CREATE INDEX idx_analysis_evidence_analysis_id ON analysis_evidence (analysis_id);
