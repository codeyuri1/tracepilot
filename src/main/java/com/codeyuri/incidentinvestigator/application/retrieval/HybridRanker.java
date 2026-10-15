package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reciprocal-rank fusion with an ACL gate applied before ranking. */
public final class HybridRanker {
    private static final int RRF_K = 60;

    public List<RetrievalResult> rank(List<RetrievalCandidate> vector, List<RetrievalCandidate> lexical,
                                      Set<String> requesterAcl, int topK, double threshold) {
        Map<String, ScoredCandidate> fused = new LinkedHashMap<>();
        add(vector, requesterAcl, fused);
        add(lexical, requesterAcl, fused);
        return fused.values().stream()
                .filter(candidate -> candidate.score >= threshold)
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
                .limit(topK)
                .map(candidate -> new RetrievalResult(candidate.candidate.chunkId(), candidate.candidate.source(),
                        candidate.candidate.excerpt(), candidate.score))
                .toList();
    }

    private void add(List<RetrievalCandidate> candidates, Set<String> requesterAcl, Map<String, ScoredCandidate> fused) {
        for (int rank = 0; rank < candidates.size(); rank++) {
            RetrievalCandidate candidate = candidates.get(rank);
            double reciprocalRank = 1.0 / (RRF_K + rank + 1);
            if (candidate.acl().stream().noneMatch(requesterAcl::contains)) continue;
            fused.compute(candidate.chunkId(), (key, current) -> new ScoredCandidate(candidate,
                    (current == null ? 0 : current.score) + reciprocalRank));
        }
    }

    private record ScoredCandidate(RetrievalCandidate candidate, double score) { }
}
