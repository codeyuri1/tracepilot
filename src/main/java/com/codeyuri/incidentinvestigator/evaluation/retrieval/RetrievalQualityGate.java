package com.codeyuri.incidentinvestigator.evaluation.retrieval;

/** Fails an evaluation run when a candidate regresses below the reviewed baseline. */
public record RetrievalQualityGate(double minimumRecallAtK, double minimumMrr, double minimumSourcePrecision) {
    public RetrievalQualityGate {
        check(minimumRecallAtK, "minimumRecallAtK");
        check(minimumMrr, "minimumMrr");
        check(minimumSourcePrecision, "minimumSourcePrecision");
    }

    public void verify(double recallAtK, double mrr, double sourcePrecision) {
        if (recallAtK < minimumRecallAtK || mrr < minimumMrr || sourcePrecision < minimumSourcePrecision) {
            throw new IllegalStateException("Retrieval quality gate failed: recall=" + recallAtK + ", mrr=" + mrr
                    + ", sourcePrecision=" + sourcePrecision);
        }
    }

    private static void check(double value, String name) {
        if (value < 0 || value > 1) throw new IllegalArgumentException(name + " must be between zero and one");
    }
}
