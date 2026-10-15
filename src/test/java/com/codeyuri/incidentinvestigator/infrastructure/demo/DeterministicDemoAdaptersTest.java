package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPrompt;
import com.codeyuri.incidentinvestigator.application.retrieval.KnowledgeIngestionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicDemoAdaptersTest {
    @Test
    void returnsEvidenceThatExistsInTheSuppliedPrompt() {
        var model = new DeterministicAnalysisModelClient();

        var output = model.analyze(new AnalysisPrompt("demo-v1", "stack trace: Connection refused"));

        assertThat(output.evidence()).extracting(item -> item.excerpt()).containsExactly("Connection refused");
    }

    @Test
    void generatesStableFiniteEmbeddingsWithTheRequiredDimension() {
        var embeddings = new DeterministicEmbeddingPort();

        float[] first = embeddings.embed("payment worker postgres runbook");
        float[] second = embeddings.embed("payment worker postgres runbook");

        assertThat(first).hasSize(KnowledgeIngestionService.EMBEDDING_DIMENSION).containsExactly(second);
        for (float value : first) assertThat(Float.isFinite(value)).isTrue();
    }
}
