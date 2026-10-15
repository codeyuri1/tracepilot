package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.List;
import java.util.Set;

public record IngestedDocument(String source, String version, String contentHash, Set<String> acl,
                               List<IngestedChunk> chunks) {
    public record IngestedChunk(int index, String content, String contentHash, float[] embedding) { }
}
