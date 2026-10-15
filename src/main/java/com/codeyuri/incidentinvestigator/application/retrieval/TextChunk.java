package com.codeyuri.incidentinvestigator.application.retrieval;

/** A deterministic, ordered fragment of a normalized document. */
public record TextChunk(int index, String content) {
}
