package com.codeyuri.incidentinvestigator.application.retrieval;

public record RetrievalResult(String chunkId, String source, String excerpt, double score) {
}
