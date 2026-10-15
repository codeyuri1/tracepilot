package com.codeyuri.incidentinvestigator.api.controller;

import com.codeyuri.incidentinvestigator.api.dto.DevelopmentTokenResponse;
import com.codeyuri.incidentinvestigator.infrastructure.security.DevelopmentJwtTokenService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Local-only bootstrap for the checked-in synthetic portfolio walkthrough. */
@RestController
@Hidden
@Profile("local")
@RequestMapping("/local/demo-token")
public class DevelopmentTokenController {
    private final DevelopmentJwtTokenService tokens;

    public DevelopmentTokenController(@Value("${app.security.jwt-secret}") String secret) {
        this.tokens = new DevelopmentJwtTokenService(secret);
    }

    @GetMapping
    public DevelopmentTokenResponse issue(@RequestParam String role, @RequestParam(defaultValue = "demo") String tenant) {
        try {
            var token = tokens.issue(role, tenant);
            return new DevelopmentTokenResponse(token.accessToken(), "Bearer", token.expiresAt());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported demo role");
        }
    }
}
