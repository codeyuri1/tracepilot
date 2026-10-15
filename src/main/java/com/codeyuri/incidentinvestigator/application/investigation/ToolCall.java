package com.codeyuri.incidentinvestigator.application.investigation;

import java.util.Map;

/** Model-originated request; registry validation rejects names and parameters not declared by a tool. */
public record ToolCall(String name, Map<String, Object> parameters) { }
