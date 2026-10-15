package com.codeyuri.incidentinvestigator.application.retrieval;

import java.util.Set;

public record KnowledgeDocument(String source, String version, String content, Set<String> acl) {
}
