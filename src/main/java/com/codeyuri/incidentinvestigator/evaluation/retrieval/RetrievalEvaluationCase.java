package com.codeyuri.incidentinvestigator.evaluation.retrieval;

import java.util.Set;

/** A manually reviewed query and its acceptable source IDs. */
public record RetrievalEvaluationCase(String query, Set<String> expectedSourceIds) { }
