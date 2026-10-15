package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.retrieval.DeterministicChunker;
import com.codeyuri.incidentinvestigator.application.retrieval.EmbeddingPort;
import com.codeyuri.incidentinvestigator.application.retrieval.HybridRanker;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeIngestionService;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeRetrievalService;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeSearch;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeStore;
import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter.JdbcKnowledgeRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"demo", "showcase"})
public class DemoConfiguration {
    @Bean DeterministicChunker demoChunker() { return new DeterministicChunker(500, 50); }
    @Bean KnowledgeIngestionService knowledgeIngestionService(DeterministicChunker chunker, EmbeddingPort embeddings, KnowledgeStore store) {
        return new KnowledgeIngestionService(chunker, embeddings, store);
    }
    @Bean KnowledgeSearch knowledgeSearch(EmbeddingPort embeddings, JdbcKnowledgeRetriever retriever) {
        return new KnowledgeRetrievalService(embeddings, retriever, new HybridRanker());
    }
}
