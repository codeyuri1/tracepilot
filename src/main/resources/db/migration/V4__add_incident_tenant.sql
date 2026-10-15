ALTER TABLE incidents ADD COLUMN tenant_id VARCHAR(100) NOT NULL DEFAULT 'public';
CREATE INDEX idx_incidents_tenant_created_at ON incidents (tenant_id, created_at DESC);
