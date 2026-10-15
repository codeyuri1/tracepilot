package com.codeyuri.incidentinvestigator.application.retrieval;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class HybridRankerTest {
    @Test void fusesStrategiesAndNeverReturnsAnUnauthorizedSource() {
        var allowed = new RetrievalCandidate("a", "runbook", "connection refused", Set.of("support"), .9);
        var forbidden = new RetrievalCandidate("b", "secret", "credential", Set.of("security"), .99);
        var results = new HybridRanker().rank(List.of(allowed, forbidden), List.of(allowed), Set.of("support"), 5, .02);
        assertThat(results).extracting(RetrievalResult::chunkId).containsExactly("a");
        assertThat(results.getFirst().score()).isGreaterThan(.02);
    }
    @Test void appliesThresholdAndTopKAfterFusion() {
        var candidate = new RetrievalCandidate("a", "runbook", "excerpt", Set.of("support"), .9);
        assertThat(new HybridRanker().rank(List.of(candidate), List.of(), Set.of("support"), 1, .02)).isEmpty();
    }
}
