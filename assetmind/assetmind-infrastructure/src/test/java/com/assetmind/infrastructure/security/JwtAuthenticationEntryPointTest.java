package com.assetmind.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationEntryPointTest {

    @Test
    void returnUnauthorizedJsonResponse() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        AuthenticationException exception = new BadCredentialsException("Bad credentials");
        entryPoint.commence(request, response, exception);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json;charset=UTF-8");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(stringWriter.toString());
        assertEquals("Unauthorized", json.get("error").asText());
        assertEquals("Bad credentials", json.get("message").asText());
        assertEquals(401, json.get("status").asInt());
    }
}
