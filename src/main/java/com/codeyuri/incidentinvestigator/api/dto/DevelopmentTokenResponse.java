package com.codeyuri.incidentinvestigator.api.dto;

import java.time.Instant;

public record DevelopmentTokenResponse(String accessToken, String tokenType, Instant expiresAt) { }
