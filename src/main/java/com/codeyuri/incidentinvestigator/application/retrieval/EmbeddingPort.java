package com.codeyuri.incidentinvestigator.application.retrieval;

/** Provider-neutral embedding boundary. */
public interface EmbeddingPort {
    float[] embed(String content);
}
