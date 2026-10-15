package com.codeyuri.incidentinvestigator.infrastructure.security;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final boolean prometheusPublic;

    public SecurityConfiguration(@Value("${app.observability.prometheus-public:false}") boolean prometheusPublic) {
        this.prometheusPublic = prometheusPublic;
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream.concat(
                        jwt.getClaimAsStringList("roles") == null ? Stream.empty()
                                : jwt.getClaimAsStringList("roles").stream().map(role -> "ROLE_" + role),
                        jwt.getClaimAsStringList("scope") == null ? Stream.empty()
                                : jwt.getClaimAsStringList("scope").stream().map(scope -> "SCOPE_" + scope))
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/local/demo-token").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/prometheus").access((authentication, context) ->
                                new org.springframework.security.authorization.AuthorizationDecision(prometheusPublic || authentication.get().getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_OPS"))))
                        .requestMatchers("/actuator/**").hasRole("OPS")
                        .requestMatchers(HttpMethod.POST, "/api/v1/demo/investigations/**").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/v1/demo/**").hasRole("INCIDENT_WRITER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/demo/**").hasRole("INCIDENT_READER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/incidents/*/analyses").hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/v1/incidents").hasRole("INCIDENT_WRITER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/incidents/**").hasRole("INCIDENT_READER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must be at least 32 bytes");
        }
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
    }
}
