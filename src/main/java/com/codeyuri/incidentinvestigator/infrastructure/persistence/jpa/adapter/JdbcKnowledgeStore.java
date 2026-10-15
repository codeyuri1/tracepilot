package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter;

import com.codeyuri.incidentinvestigator.application.retrieval.IngestedDocument;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** PostgreSQL/pgvector adapter; vector literals are supplied only from validated float arrays. */
@Repository
public class JdbcKnowledgeStore implements KnowledgeStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcKnowledgeStore(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public boolean documentExistsByHash(String contentHash) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from knowledge_documents where content_hash = ?)", Boolean.class, contentHash));
    }

    @Override
    @Transactional
    public void save(IngestedDocument document) {
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update("insert into knowledge_documents (id, source, source_version, content_hash, acl, created_at) values (?, ?, ?, ?, ?, ?)",
                documentId, document.source(), document.version(), document.contentHash(), document.acl().toArray(String[]::new), java.sql.Timestamp.from(Instant.now()));
        for (IngestedDocument.IngestedChunk chunk : document.chunks()) {
            jdbcTemplate.update("insert into knowledge_chunks (id, document_id, chunk_index, content, content_hash, embedding, metadata) values (?, ?, ?, ?, ?, cast(? as vector), '{}'::jsonb)",
                    UUID.randomUUID(), documentId, chunk.index(), chunk.content(), chunk.contentHash(), vectorLiteral(chunk.embedding()));
        }
    }

    private String vectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) literal.append(',');
            literal.append(embedding[index]);
        }
        return literal.append(']').toString();
    }
}
