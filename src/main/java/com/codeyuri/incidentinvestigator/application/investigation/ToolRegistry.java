package com.codeyuri.incidentinvestigator.application.investigation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolRegistry {
    private final Map<String, InvestigationTool> tools;
    public ToolRegistry(Set<InvestigationTool> tools) { this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(InvestigationTool::name, tool -> tool)); }
    public InvestigationTool resolve(ToolCall call) {
        InvestigationTool tool = tools.get(call.name());
        if (tool == null) throw new IllegalArgumentException("Tool is not allowed");
        return tool;
    }
}
