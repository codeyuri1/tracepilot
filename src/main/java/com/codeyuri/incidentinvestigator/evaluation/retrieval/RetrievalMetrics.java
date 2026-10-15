package com.codeyuri.incidentinvestigator.evaluation.retrieval;

import java.util.List;
import java.util.Set;

public final class RetrievalMetrics {
    public double recallAtK(Set<String> expected, List<String> retrieved, int k) {
        return expected.isEmpty() ? 1 : (double) retrieved.stream().limit(k).filter(expected::contains).distinct().count() / expected.size();
    }
    public double reciprocalRank(Set<String> expected, List<String> retrieved) {
        for (int index = 0; index < retrieved.size(); index++) if (expected.contains(retrieved.get(index))) return 1.0 / (index + 1);
        return 0;
    }
    public double sourcePrecision(Set<String> expected, List<String> retrieved) {
        return retrieved.isEmpty() ? 1 : (double) retrieved.stream().filter(expected::contains).count() / retrieved.size();
    }
}
