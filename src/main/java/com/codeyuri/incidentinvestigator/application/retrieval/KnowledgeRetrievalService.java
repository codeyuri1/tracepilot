package com.codeyuri.incidentinvestigator.application.retrieval;

import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter.JdbcKnowledgeRetriever;
import java.util.List;
import java.util.Set;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Retrieves only authorized indexed chunks; callers pass source IDs, never full documents, to models. */
public final class KnowledgeRetrievalService implements KnowledgeSearch {
    private final EmbeddingPort embeddings; private final JdbcKnowledgeRetriever store; private final HybridRanker ranker;
    private final MeterRegistry meterRegistry;
    public KnowledgeRetrievalService(EmbeddingPort embeddings, JdbcKnowledgeRetriever store, HybridRanker ranker) { this(embeddings, store, ranker, null); }
    public KnowledgeRetrievalService(EmbeddingPort embeddings, JdbcKnowledgeRetriever store, HybridRanker ranker, MeterRegistry meterRegistry) { this.embeddings=embeddings; this.store=store; this.ranker=ranker; this.meterRegistry=meterRegistry; }
    @Override
    public List<RetrievalResult> retrieve(String query, Set<String> acl, int topK, double threshold) {
        float[] vector=embeddings.embed(query);
        if(vector==null || vector.length!=KnowledgeIngestionService.EMBEDDING_DIMENSION) throw new IllegalArgumentException("Embedding dimension must be 384");
        int candidates=Math.max(topK*3, topK);
        long started = System.nanoTime();
        List<RetrievalResult> results = ranker.rank(store.vector(vector,acl,candidates),store.lexical(query,acl,candidates),acl,topK,threshold);
        if (meterRegistry != null) {
            Timer.builder("incident_investigator.retrieval.latency").tag("outcome", "success").register(meterRegistry)
                    .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
            meterRegistry.summary("incident_investigator.retrieval.results").record(results.size());
        }
        return results;
    }
}
