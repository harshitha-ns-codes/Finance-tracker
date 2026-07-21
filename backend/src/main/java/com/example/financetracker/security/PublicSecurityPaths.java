package com.example.financetracker.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Paths that must bypass JWT authentication and require no existing session.
 */
final class PublicSecurityPaths {

    static final String[] PERMIT_ALL = {
            "/",
            "/api/health",
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    };

    private PublicSecurityPaths() {
    }

    static boolean matches(HttpServletRequest request) {
        String path = normalizePath(request);
        for (String allowed : PERMIT_ALL) {
            if (allowed.endsWith("/**")) {
                String prefix = allowed.substring(0, allowed.length() - 3);
                if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                    return true;
                }
            } else if (path.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    static String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri.isEmpty() ? "/" : uri;
    }
}
