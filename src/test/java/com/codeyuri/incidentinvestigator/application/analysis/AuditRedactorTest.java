package com.codeyuri.incidentinvestigator.application.analysis;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditRedactorTest {
    private final AuditRedactor redactor = new AuditRedactor();

    @Test
    void redactsQuotedJsonSecretsAndPreservesValidJson() throws Exception {
        String result = redactor.redact("{\"password\":\"two words\",\"access_token\":\"demo-token\",\"safe\":\"Connection refused\"}");
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result);
        assertThat(json.get("password").asText()).isEqualTo("[REDACTED]");
        assertThat(json.get("access_token").asText()).isEqualTo("[REDACTED]");
        assertThat(json.get("safe").asText()).isEqualTo("Connection refused");
    }

    @Test
    void redactsAuthorizationUrlCredentialsAndPersonalEmail() {
        String result = redactor.redact("Authorization: Basic ZGVtbzpwYXNz https://demo:synthetic-pass@example.test/api?api_key=demo-key user=person@example.test");
        assertThat(result).doesNotContain("ZGVtbzpwYXNz", "synthetic-pass", "demo-key", "person@example.test");
        assertThat(result).contains("https://", "[REDACTED]", "/api");
    }

    @Test
    void redactionIsIdempotentAndHandlesNull() {
        assertThat(redactor.redact(null)).isNull();
        String once = redactor.redact("password=demo-pass Authorization: Bearer abc.def.ghi");
        assertThat(redactor.redact(once)).isEqualTo(once);
    }

    @Test
    void redactsMultilineQuotedSecrets() {
        assertThat(redactor.redact("password=\"first\nsecond\" Connection refused"))
                .doesNotContain("first", "second").contains("Connection refused", "[REDACTED]");
    }

    @Test
    void handlesMaximumSizeQuotedSecretsWithoutOverflow() {
        String secret = "x".repeat(49_000);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(3), () ->
                assertThat(redactor.redact("{\"password\":\"" + secret + "\"}"))
                        .isEqualTo("{\"password\":\"[REDACTED]\"}"));
    }

    @Test
    void removesCommonCredentialsBeforeAuditPersistenceOrTelemetry() {
        String value = "token=abc password: hunter2 Authorization=Bearer abc.def postgres://user:pw@db/app";
        String redacted = redactor.redact(value);
        assertThat(redacted).contains("[REDACTED]")
                .doesNotContain("hunter2").doesNotContain("abc.def").doesNotContain(":pw@");
    }
}
