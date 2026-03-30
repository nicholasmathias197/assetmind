package com.assetmind.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for AI endpoints.
 * Limits each client (by IP or authenticated principal) to a fixed number
 * of requests per sliding window.
 */
@Component
public class AiRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static final String[] AI_PATHS = {
            "/api/v1/classification/suggest",
            "/api/v1/depreciation/recommend",
            "/api/v1/depreciation/ai-run",
            "/api/v1/tax-strategy/recommend",
            "/api/v1/breakout/suggest"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!isAiEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(MAX_REQUESTS, WINDOW_MS));

        if (bucket.tryConsume()) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.remaining()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(WINDOW_MS / 1000));
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many AI requests. Please try again later.\"}"
            );
        }
    }

    private boolean isAiEndpoint(String path) {
        for (String aiPath : AI_PATHS) {
            if (path.equals(aiPath)) return true;
        }
        return false;
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Prefer authenticated user principal, fall back to IP
        var principal = request.getUserPrincipal();
        if (principal != null) {
            return "user:" + principal.getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Simple sliding-window token bucket.
     */
    static class TokenBucket {
        private final int maxTokens;
        private final long windowMs;
        private final AtomicInteger tokens;
        private volatile long windowStart;

        TokenBucket(int maxTokens, long windowMs) {
            this.maxTokens = maxTokens;
            this.windowMs = windowMs;
            this.tokens = new AtomicInteger(maxTokens);
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            maybeReset();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        int remaining() {
            maybeReset();
            return Math.max(0, tokens.get());
        }

        private void maybeReset() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                tokens.set(maxTokens);
                windowStart = now;
            }
        }
    }
}
