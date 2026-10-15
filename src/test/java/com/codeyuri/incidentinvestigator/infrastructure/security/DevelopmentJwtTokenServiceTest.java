package com.codeyuri.incidentinvestigator.infrastructure.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevelopmentJwtTokenServiceTest {
    private static final String SECRET = "development-test-signing-secret-that-is-long-enough";

    @Test
    void issuesAnHs256TokenForTheRequestedAllowedRole() throws Exception {
        Class<?> type = Class.forName("com.codeyuri.incidentinvestigator.infrastructure.security.DevelopmentJwtTokenService");
        Object service = type.getConstructor(String.class).newInstance(SECRET);
        Method issue = type.getMethod("issue", String.class);

        Object token = issue.invoke(service, "INCIDENT_WRITER");
        String accessToken = (String) token.getClass().getMethod("accessToken").invoke(token);

        String[] sections = accessToken.split("\\.");
        assertThat(sections).hasSize(3);
        assertThat(json(sections[0])).contains("\"alg\":\"HS256\"");
        assertThat(json(sections[1])).contains("\"sub\":\"portfolio-demo\"")
                .contains("\"tenant\":\"demo\"")
                .contains("\"roles\":[\"INCIDENT_WRITER\"]");
        assertThat(signature(sections[0] + "." + sections[1])).isEqualTo(sections[2]);
    }

    @Test
    void rejectsRolesOutsideTheLocalDemoAllowlist() throws Exception {
        Class<?> type = Class.forName("com.codeyuri.incidentinvestigator.infrastructure.security.DevelopmentJwtTokenService");
        Object service = type.getConstructor(String.class).newInstance(SECRET);
        Method issue = type.getMethod("issue", String.class);

        assertThatThrownBy(() -> issue.invoke(service, "ADMIN"))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canIssueAnIsolatedSyntheticTenantToken() throws Exception {
        Class<?> type = Class.forName("com.codeyuri.incidentinvestigator.infrastructure.security.DevelopmentJwtTokenService");
        Object service = type.getConstructor(String.class).newInstance(SECRET);
        Method issue = type.getMethod("issue", String.class, String.class);

        Object token = issue.invoke(service, "INCIDENT_READER", "other-demo");
        String accessToken = (String) token.getClass().getMethod("accessToken").invoke(token);

        assertThat(json(accessToken.split("\\.")[1])).contains("\"tenant\":\"other-demo\"");
    }

    private String json(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private String signature(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }
}
