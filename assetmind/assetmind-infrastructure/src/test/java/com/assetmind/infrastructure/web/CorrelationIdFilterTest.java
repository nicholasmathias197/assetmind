package com.assetmind.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenNoneProvided() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Correlation-ID"), argThat(s -> s != null && s.length() == 36));
        verify(chain).doFilter(request, response);
        assertNull(MDC.get("correlationId"), "MDC should be cleaned up after filtering");
    }

    @Test
    void generatesCorrelationIdWhenBlank() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-ID")).thenReturn("   ");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Correlation-ID"), argThat(s -> s != null && !s.isBlank()));
        verify(chain).doFilter(request, response);
    }

    @Test
    void usesExistingCorrelationId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-ID")).thenReturn("existing-id-123");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Correlation-ID", "existing-id-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void cleansMdcEvenOnException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, chain));
        assertNull(MDC.get("correlationId"), "MDC should be cleaned up even on exception");
    }
}
