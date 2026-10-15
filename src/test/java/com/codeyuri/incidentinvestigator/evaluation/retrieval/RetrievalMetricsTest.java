package com.codeyuri.incidentinvestigator.evaluation.retrieval;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class RetrievalMetricsTest {
    @Test void calculatesRecallMrrAndSourcePrecision() {
        var metrics = new RetrievalMetrics(); var expected = Set.of("runbook-db", "incident-42");
        var retrieved = List.of("other", "incident-42", "runbook-db");
        assertThat(metrics.recallAtK(expected, retrieved, 2)).isEqualTo(.5);
        assertThat(metrics.reciprocalRank(expected, retrieved)).isEqualTo(.5);
        assertThat(metrics.sourcePrecision(expected, retrieved)).isEqualTo(2.0 / 3.0);
    }
}
