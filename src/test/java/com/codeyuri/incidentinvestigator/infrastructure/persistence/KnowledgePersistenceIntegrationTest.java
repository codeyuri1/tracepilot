package com.codeyuri.incidentinvestigator.infrastructure.persistence;

import com.codeyuri.incidentinvestigator.application.retrieval.*;
import com.codeyuri.incidentinvestigator.infrastructure.demo.DeterministicEmbeddingPort;
import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter.JdbcKnowledgeStore;
import com.codeyuri.incidentinvestigator.infrastructure.persistence.jpa.adapter.JdbcKnowledgeRetriever;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class KnowledgePersistenceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcKnowledgeStore store;
    @Autowired JdbcKnowledgeRetriever retriever;

    @Test void persistsAndRetrievesIdenticalChunksWithinDocumentsAndAcrossTenants() {
        var embeddings = new DeterministicEmbeddingPort();
        var service = new KnowledgeIngestionService(new DeterministicChunker(20, 0), embeddings, store);
        String content = "PostgreSQL refused. ".repeat(3);
        assertThat(service.ingest(new KnowledgeDocument("a-runbook", "v1", content, Set.of("a")))).isTrue();
        assertThat(service.ingest(new KnowledgeDocument("b-runbook", "v1", content, Set.of("b")))).isTrue();
        assertThat(service.ingest(new KnowledgeDocument("b-runbook", "v1", content, Set.of("b")))).isFalse();
        assertThat(retriever.lexical("PostgreSQL", Set.of("a"), 10)).isNotEmpty()
                .allSatisfy(candidate -> assertThat(candidate.source()).isEqualTo("a-runbook"));
        assertThat(retriever.vector(embeddings.embed("PostgreSQL"), Set.of("b"), 10)).isNotEmpty()
                .allSatisfy(candidate -> assertThat(candidate.source()).isEqualTo("b-runbook"));
        assertThat(retriever.lexical("PostgreSQL", Set.of("c"), 10)).isEmpty();
    }
}
