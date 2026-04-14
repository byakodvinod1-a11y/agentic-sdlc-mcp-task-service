package org.acn.mcptaskservice.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Set<String> INVALID_VALUES = Set.of(
            "change-me",
            "changeme",
            "your-secret-key",
            "default",
            "test",
            "dummy"
    );

    private final String apiKey;

    public ApiKeyFilter(@Value("${app.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.api-key property is not set");
        }

        if (INVALID_VALUES.contains(apiKey.trim().toLowerCase())) {
            throw new IllegalStateException("app.api-key must not use a placeholder/default value");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (
                uri.startsWith("/actuator/health") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs")
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String key = request.getHeader("X-API-KEY");

        if (apiKey.equals(key)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid X-API-KEY");
    }
}