package com.codeyuri.incidentinvestigator.application.analysis;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPromptFactoryTest {

    @Test
    void instructsTheModelToReturnHumanReadableAnalysisInPortuguese() {
        var incident = Incident.receive("Falha no pagamento", "Connection refused", "payment-worker", Severity.HIGH, "demo");

        var prompt = new AnalysisPromptFactory("v1").create(incident);

        assertThat(prompt.content())
                .contains("português")
                .contains("summary")
                .contains("probableCause")
                .contains("recommendations")
                .contains("Connection refused")
                .contains("Severidade informada: HIGH")
                .contains("Investigar:", "Mitigar:", "Validar:");
    }
    @Test void suppliesTypedSchemaForCompatibleModels() throws Exception {
        var incident = Incident.receive("Falha no banco", "Connection refused", "worker", Severity.HIGH, "demo");
        String content = new AnalysisPromptFactory("v3").create(incident).content();
        assertThat(content).contains("JSON_SCHEMA:", "END_JSON_SCHEMA");
        String schema = content.substring(content.indexOf("JSON_SCHEMA:") + "JSON_SCHEMA:".length(), content.indexOf("END_JSON_SCHEMA")).trim();
        var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(schema);
        assertThat(root.path("properties").path("confidence").path("type").asText()).isEqualTo("number");
        assertThat(root.path("properties").path("recommendations").path("items").path("type").asText()).isEqualTo("string");
    }

}
