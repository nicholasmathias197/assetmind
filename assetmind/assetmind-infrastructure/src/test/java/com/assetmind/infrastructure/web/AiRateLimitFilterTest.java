package com.assetmind.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiRateLimitFilterTest {

    private final AiRateLimitFilter filter = new AiRateLimitFilter();

    @Test
    void nonAiEndpointPassesThrough() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/assets");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void aiEndpointAllowedUnderLimit() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/classification/suggest");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("20"));
    }

    @Test
    void aiEndpointBlockedWhenLimitExceeded() throws Exception {
        // Exhaust 20 requests from a unique IP
        for (int i = 0; i < 20; i++) {
            HttpServletRequest req = mock(HttpServletRequest.class);
            HttpServletResponse resp = mock(HttpServletResponse.class);
            FilterChain ch = mock(FilterChain.class);
            when(req.getRequestURI()).thenReturn("/api/v1/depreciation/recommend");
            when(req.getRemoteAddr()).thenReturn("192.168.99.99");
            filter.doFilterInternal(req, resp, ch);
        }

        // 21st request should be rate limited
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = new PrintWriter(new StringWriter());
        when(response.getWriter()).thenReturn(writer);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/depreciation/recommend");
        when(request.getRemoteAddr()).thenReturn("192.168.99.99");

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(429);
        verify(chain, never()).doFilter(request, response);
        verify(response).setHeader("X-RateLimit-Remaining", "0");
    }

    @Test
    void allFiveAiPathsAreRecognized() throws Exception {
        String[] aiPaths = {
                "/api/v1/classification/suggest",
                "/api/v1/depreciation/recommend",
                "/api/v1/depreciation/ai-run",
                "/api/v1/tax-strategy/recommend",
                "/api/v1/breakout/suggest"
        };
        for (int i = 0; i < aiPaths.length; i++) {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            when(request.getRequestURI()).thenReturn(aiPaths[i]);
            when(request.getRemoteAddr()).thenReturn("unique-ip-" + i);

            filter.doFilterInternal(request, response, chain);

            verify(response).setHeader(eq("X-RateLimit-Limit"), eq("20"));
        }
    }

    @Test
    void usesAuthenticatedPrincipalAsClientKey() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");
        when(request.getRequestURI()).thenReturn("/api/v1/classification/suggest");
        when(request.getUserPrincipal()).thenReturn(principal);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void usesXForwardedForWhenNoPrincipal() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/v1/breakout/suggest");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenBucketResetsAfterWindow() {
        AiRateLimitFilter.TokenBucket bucket = new AiRateLimitFilter.TokenBucket(2, 1); // 1ms window
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());

        // Wait for window to expire
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        assertTrue(bucket.tryConsume(), "Bucket should reset after window expires");
    }

    @Test
    void tokenBucketRemainingReflectsConsumption() {
        AiRateLimitFilter.TokenBucket bucket = new AiRateLimitFilter.TokenBucket(5, 60_000);
        assertEquals(5, bucket.remaining());
        bucket.tryConsume();
        assertEquals(4, bucket.remaining());
    }
}
