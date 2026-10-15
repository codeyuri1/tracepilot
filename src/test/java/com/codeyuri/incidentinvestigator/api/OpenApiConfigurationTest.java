package com.codeyuri.incidentinvestigator.api;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigurationTest {
    @Test
    void declaresBearerAuthenticationForThePublicApiContract() throws Exception {
        Class<?> type = Class.forName("com.codeyuri.incidentinvestigator.api.OpenApiConfiguration");
        Object configuration = type.getConstructor().newInstance();
        OpenAPI openApi = (OpenAPI) type.getMethod("openApi").invoke(configuration);

        assertThat(openApi.getInfo().getTitle()).isEqualTo("TracePilot API");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearerAuth").getScheme()).isEqualTo("bearer");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat()).isEqualTo("JWT");
    }
}
