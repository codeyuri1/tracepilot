package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.retrieval.EmbeddingPort;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeIngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable 384-dimensional vectors for demo fixtures; never use for semantic production search. */
@Component
@Profile({"demo", "showcase"})
public class DeterministicEmbeddingPort implements EmbeddingPort {
    @Override
    public float[] embed(String content) {
        byte[] source = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        float[] embedding = new float[KnowledgeIngestionService.EMBEDDING_DIMENSION];
        for (int index = 0; index < embedding.length; index++) {
            byte[] digest = digest(source, index);
            embedding[index] = ((digest[0] & 0xff) / 127.5f) - 1.0f;
        }
        return embedding;
    }

    private byte[] digest(byte[] source, int index) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(source);
            digest.update((byte) (index >>> 24));
            digest.update((byte) (index >>> 16));
            digest.update((byte) (index >>> 8));
            digest.update((byte) index);
            return digest.digest();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
