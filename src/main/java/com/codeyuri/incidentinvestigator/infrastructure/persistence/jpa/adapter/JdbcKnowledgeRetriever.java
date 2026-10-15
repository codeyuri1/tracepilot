package com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter;

import com.codeyuri.incidentinvestigator.application.retrieval.RetrievalCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/** ACL-filtered PostgreSQL full-text and pgvector candidate queries. */
@Repository
public class JdbcKnowledgeRetriever {
    private final JdbcTemplate jdbc;
    public JdbcKnowledgeRetriever(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<RetrievalCandidate> vector(float[] embedding, Set<String> acl, int limit) {
        return jdbc.query("select c.id::text, d.source, c.content, d.acl, 1 - (c.embedding <=> cast(? as vector)) score from knowledge_chunks c join knowledge_documents d on d.id=c.document_id where d.acl && cast(? as text[]) order by c.embedding <=> cast(? as vector) limit ?",
                (rs, row) -> new RetrievalCandidate(rs.getString(1), rs.getString(2), rs.getString(3), Set.of((String[]) rs.getArray(4).getArray()), rs.getDouble(5)), vector(embedding), acl.toArray(String[]::new), vector(embedding), limit);
    }
    public List<RetrievalCandidate> lexical(String query, Set<String> acl, int limit) {
        return jdbc.query("select c.id::text, d.source, c.content, d.acl, ts_rank(c.search_vector, plainto_tsquery('simple', ?)) score from knowledge_chunks c join knowledge_documents d on d.id=c.document_id where d.acl && cast(? as text[]) and c.search_vector @@ plainto_tsquery('simple', ?) order by score desc limit ?",
                (rs, row) -> new RetrievalCandidate(rs.getString(1), rs.getString(2), rs.getString(3), Set.of((String[]) rs.getArray(4).getArray()), rs.getDouble(5)), query, acl.toArray(String[]::new), query, limit);
    }
    private String vector(float[] values) { StringBuilder out = new StringBuilder("["); for (int i=0;i<values.length;i++) { if(i>0) out.append(','); out.append(values[i]); } return out.append(']').toString(); }
}
