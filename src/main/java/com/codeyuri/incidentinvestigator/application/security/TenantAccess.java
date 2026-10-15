package com.codeyuri.incidentinvestigator.application.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the tenant exclusively from an authenticated token, never from request input. */
public final class TenantAccess {
    public static final String DEFAULT_TENANT = "public";

    private TenantAccess() { }

    public static String currentTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return DEFAULT_TENANT;
        }
        String tenant = jwt.getClaimAsString("tenant");
        if (tenant == null || tenant.isBlank() || tenant.length() > 100) {
            throw new IllegalStateException("Authenticated token does not contain a valid tenant claim");
        }
        return tenant;
    }
}
