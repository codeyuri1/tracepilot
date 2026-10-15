package com.codeyuri.incidentinvestigator.application.retrieval;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeIngestionServiceTest {
    @Test void ingestsOnceAndKeepsMetadata() {
        var store = new Store();
        var service = new KnowledgeIngestionService(new DeterministicChunker(20, 2), text -> new float[384], store);
        var document = new KnowledgeDocument("runbook", "v1", "database connection refused", Set.of("support"));
        assertThat(service.ingest(document)).isTrue();
        assertThat(service.ingest(document)).isFalse();
        assertThat(store.document.acl()).containsExactly("support");
        assertThat(store.document.chunks()).isNotEmpty();
    }
    @Test void rejectsWrongEmbeddingDimension() {
        var service = new KnowledgeIngestionService(new DeterministicChunker(20, 2), text -> new float[3], new Store());
        assertThatThrownBy(() -> service.ingest(new KnowledgeDocument("s", "v", "content", Set.of("a"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sanitizesBeforeEmbeddingAndPersistence() {
        var store = new Store();
        var service = new KnowledgeIngestionService(new DeterministicChunker(500, 20), text -> {
            assertThat(text).doesNotContain("synthetic-secret", "person@example.test");
            return new float[384];
        }, store);
        service.ingest(new KnowledgeDocument("runbook", "v1", "Connection refused password=synthetic-secret contact person@example.test", Set.of("demo")));
        assertThat(store.document.chunks()).allSatisfy(chunk ->
                assertThat(chunk.content()).doesNotContain("synthetic-secret", "person@example.test"));
    }

    @Test void identicalContentIsIndependentlyAvailableToDifferentTenants() {
        var store = new Store();
        var service = new KnowledgeIngestionService(new DeterministicChunker(500, 20), text -> new float[384], store);
        assertThat(service.ingest(new KnowledgeDocument("a-runbook", "v1", "Connection refused", Set.of("tenant-a")))).isTrue();
        assertThat(service.ingest(new KnowledgeDocument("b-runbook", "v1", "Connection refused", Set.of("tenant-b")))).isTrue();
        assertThat(service.ingest(new KnowledgeDocument("b-runbook", "v1", "Connection refused", Set.of("tenant-b")))).isFalse();
    }

    static class Store implements KnowledgeStore {
        IngestedDocument document; Set<String> hashes = new HashSet<>();
        public boolean documentExistsByHash(String hash) { return hashes.contains(hash); }
        public void save(IngestedDocument value) { document = value; hashes.add(value.contentHash()); }
    }
}
