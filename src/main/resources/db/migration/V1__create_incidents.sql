CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    stack_trace TEXT NOT NULL,
    source VARCHAR(100) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_incidents_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incidents_status CHECK (status IN ('RECEIVED', 'ANALYZING', 'ANALYZED', 'FAILED'))
);

CREATE INDEX idx_incidents_created_at ON incidents (created_at DESC);
