package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.Set;

/** Candidate returned by one retrieval strategy before access control and fusion. */
public record RetrievalCandidate(String chunkId, String source, String excerpt, Set<String> acl, double score) {
}
