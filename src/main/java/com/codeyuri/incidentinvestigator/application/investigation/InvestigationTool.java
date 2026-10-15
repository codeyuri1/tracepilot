package com.codeyuri.incidentinvestigator.application.investigation;

import java.util.Map;

public interface InvestigationTool {
    String name();
    Map<String, Object> execute(Map<String, Object> parameters, String correlationId);
}
