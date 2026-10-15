CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY,
    source VARCHAR(255) NOT NULL,
    source_version VARCHAR(120) NOT NULL,
    content_hash CHAR(64) NOT NULL UNIQUE,
    acl TEXT[] NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL UNIQUE,
    embedding vector(384) NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_knowledge_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunks_embedding ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_knowledge_chunks_lexical ON knowledge_chunks USING gin (search_vector);
CREATE INDEX idx_knowledge_documents_acl ON knowledge_documents USING gin (acl);
