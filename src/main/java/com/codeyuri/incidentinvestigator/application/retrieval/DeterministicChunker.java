package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.ArrayList;
import java.util.List;

/** Splits normalized text by character count while preserving configured overlap. */
public final class DeterministicChunker {
    private final int chunkSize;
    private final int overlap;

    public DeterministicChunker(int chunkSize, int overlap) {
        if (chunkSize < 1 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("chunkSize must be positive and overlap must be smaller than chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<TextChunk> chunk(String document) {
        String normalized = normalize(document);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(new TextChunk(chunks.size(), normalized.substring(start, end)));
            if (end == normalized.length()) {
                break;
            }
            start = end - overlap;
        }
        return List.copyOf(chunks);
    }

    private String normalize(String document) {
        if (document == null) {
            return "";
        }
        return document.replace("\r\n", "\n").replace('\r', '\n').trim().replaceAll("[ \\t]+", " ");
    }
}
