package org.acn.mcptaskservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey;

    public ApiKeyFilter(@Value("${app.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.api-key property is not set");
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