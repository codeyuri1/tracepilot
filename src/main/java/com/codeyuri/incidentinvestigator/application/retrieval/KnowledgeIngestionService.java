package com.codeyuri.incidentinvestigator.application.retrieval;

import com.codeyuri.incidentinvestigator.application.analysis.AuditRedactor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class KnowledgeIngestionService {
    public static final int EMBEDDING_DIMENSION = 384;
    private final DeterministicChunker chunker;
    private final EmbeddingPort embeddings;
    private final KnowledgeStore store;

    public KnowledgeIngestionService(DeterministicChunker chunker, EmbeddingPort embeddings, KnowledgeStore store) {
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.store = store;
    }

    public boolean ingest(KnowledgeDocument document) {
        var redactor = new AuditRedactor();
        String content = redactor.redact(document.content());
        // Length-prefix ACL entries to avoid ambiguous tenant-set concatenation.
        String scope = document.acl().stream().sorted().map(value -> value.length() + ":" + value)
                .collect(java.util.stream.Collectors.joining());
        String documentHash = sha256(scope + "\n" + content);
        if (store.documentExistsByHash(documentHash)) return false;
        List<IngestedDocument.IngestedChunk> chunks = chunker.chunk(content).stream().map(chunk -> {
            float[] embedding = embeddings.embed(chunk.content());
            if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
                throw new IllegalArgumentException("Embedding dimension must be " + EMBEDDING_DIMENSION);
            }
            return new IngestedDocument.IngestedChunk(chunk.index(), chunk.content(), sha256(chunk.content()), embedding);
        }).toList();
        store.save(new IngestedDocument(redactor.redact(document.source()), redactor.redact(document.version()), documentHash, document.acl(), chunks));
        return true;
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte item : bytes) hex.append("%02x".formatted(item));
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
