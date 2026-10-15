package com.codeyuri.incidentinvestigator.application.retrieval;

/** Persistence boundary for idempotent knowledge ingestion. */
public interface KnowledgeStore {
    boolean documentExistsByHash(String contentHash);
    void save(IngestedDocument document);
}
