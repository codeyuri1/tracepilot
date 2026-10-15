package com.codeyuri.incidentinvestigator.application.analysis;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/** Best-effort defense for known credential formats; never a substitute for synthetic demo data. */
@Component
public class AuditRedactor {
    private static final String KEYS = "(?:api[_-]?key|(?:access[_-]?|refresh[_-]?)?token|password|passwd|secret|client[_-]?secret|authorization)";
    private static final Pattern QUOTED = Pattern.compile(
            "(?is)([\"']?" + KEYS + "[\"']?\\s*[:=]\\s*)([\"'])(?:\\\\.|(?!\\2).)*+\\2");
    private static final Pattern AUTH = Pattern.compile("(?i)\\b(?:bearer|basic)\\s+(?!\\[REDACTED\\])[a-z0-9._~+/-]+=*");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(\\b" + KEYS + "\\s*[:=]\\s*)(?![\"'])([^\\s,};&\"']+)");
    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "(?i)([a-z][a-z0-9+.-]{0,31}://)[^\\s/@]+:[^\\s/@]+@");
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]{1,64}@[a-zA-Z0-9.-]{1,253}\\.[a-zA-Z]{2,}");

    public String redact(String value) {
        if (value == null) return null;
        String redacted = URL_CREDENTIALS.matcher(value).replaceAll("$1[REDACTED]@");
        redacted = EMAIL.matcher(redacted).replaceAll("[REDACTED]");
        redacted = AUTH.matcher(redacted).replaceAll("[REDACTED]");
        redacted = QUOTED.matcher(redacted).replaceAll("$1$2[REDACTED]$2");
        return SECRET.matcher(redacted).replaceAll("$1[REDACTED]");
    }
}
