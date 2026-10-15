package com.codeyuri.incidentinvestigator.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/** Issues short-lived synthetic tokens only for the local portfolio walkthrough. */
public final class DevelopmentJwtTokenService {
    private static final Set<String> ALLOWED_ROLES = Set.of("INCIDENT_WRITER", "INCIDENT_READER", "ANALYST", "OPS");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final byte[] secret;

    public DevelopmentJwtTokenService(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public IssuedToken issue(String role) {
        return issue(role, "demo");
    }

    public IssuedToken issue(String role, String tenant) {
        if (!ALLOWED_ROLES.contains(role)) throw new IllegalArgumentException("Unsupported demo role");
        if (tenant == null || !tenant.matches("[a-z0-9][a-z0-9-]{0,99}")) throw new IllegalArgumentException("Unsupported demo tenant");
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encode(Map.of("sub", "portfolio-demo", "tenant", tenant, "roles", Set.of(role),
                "iat", Instant.now().getEpochSecond(), "exp", expiresAt.getEpochSecond()));
        String unsignedToken = header + "." + payload;
        return new IssuedToken(unsignedToken + "." + signature(unsignedToken), expiresAt);
    }

    private String encode(Map<String, ?> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode demo JWT", exception);
        }
    }

    private String signature(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign demo JWT", exception);
        }
    }

    public record IssuedToken(String accessToken, Instant expiresAt) { }
}
