package com.example.financetracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    /** Vite local defaults — always merged into the final allow-list. */
    private static final List<String> VITE_LOCAL_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174"
    );

    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        Set<String> merged = new LinkedHashSet<>(VITE_LOCAL_ORIGINS);
        for (String origin : allowedOrigins) {
            if (origin != null && !origin.isBlank()) {
                merged.add(normalize(origin));
            }
        }
        return List.copyOf(merged);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? new ArrayList<>()
                : allowedOrigins.stream()
                        .filter(origin -> origin != null && !origin.isBlank())
                        .map(AppCorsProperties::normalize)
                        .distinct()
                        .toList();
    }

    private static String normalize(String origin) {
        String trimmed = origin.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
