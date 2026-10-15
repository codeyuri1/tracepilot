package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPrompt;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeterministicAnalysisModelClientTest {
    @Test
    void supportsEverySimulatorScenarioWithLiteralEvidence() {
        var model = new DeterministicAnalysisModelClient();
        String[][] cases = {
                {"Connection refused", "DATABASE_CONNECTIVITY"},
                {"java.net.SocketTimeoutException: Read timed out", "DEPENDENCY_TIMEOUT"},
                {"HTTP 503 Service Unavailable from identity-provider", "DEPENDENCY_UNAVAILABLE"},
                {"java.lang.IllegalStateException: order state is invalid", "INVALID_STATE"},
                {"demo.cache.CacheMiss: cache miss; fallback completed", "RECOVERED_CACHE_MISS"}
        };
        for (String[] entry : cases) {
            var output = model.analyze(new AnalysisPrompt("test", entry[0]));
            assertThat(output.category()).isEqualTo(entry[1]);
            assertThat(output.recommendations()).hasSize(3);
            assertThat(output.recommendations().get(0)).startsWith("Investigar:");
            assertThat(output.recommendations().get(1)).startsWith("Mitigar:");
            assertThat(output.recommendations().get(2)).startsWith("Validar:");
            assertThat(output.evidence()).allSatisfy(evidence -> assertThat(entry[0]).contains(evidence.excerpt()));
        }
    }
    @Test void distinguishesRecoveredCacheMissFromUnprovenOutage() {
        var output = new DeterministicAnalysisModelClient().analyze(new AnalysisPrompt("v2", "cache miss; fallback completed"));
        assertThat(output.category()).isEqualTo("RECOVERED_CACHE_MISS");
        assertThat(output.summary()).contains("fallback");
        assertThat(output.probableCause()).contains("Não há evidência de indisponibilidade");
    }
}
