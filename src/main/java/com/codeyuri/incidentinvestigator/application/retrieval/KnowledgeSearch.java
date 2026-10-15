package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.List;
import java.util.Set;

/** Authorized search boundary consumed by investigation tools. */
public interface KnowledgeSearch {
    List<RetrievalResult> retrieve(String query, Set<String> acl, int topK, double threshold);
}
