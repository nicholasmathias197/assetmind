package com.assetmind.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider() {
        return new JwtTokenProvider("test-secret-for-jwt-unit-tests", 3600000, 86400000);
    }

    @Test
    void generateAndValidateAccessToken() {
        JwtTokenProvider provider = provider();
        String token = provider.generateAccessToken("user-1", "admin", "ADMIN");

        assertTrue(provider.validateToken(token));
        assertEquals("user-1", provider.getUserIdFromToken(token));
        assertEquals("ADMIN", provider.getRoleFromToken(token));
    }

    @Test
    void generateAndValidateRefreshToken() {
        JwtTokenProvider provider = provider();
        String token = provider.generateRefreshToken("user-2");

        assertTrue(provider.validateToken(token));
        assertEquals("user-2", provider.getUserIdFromToken(token));
    }

    @Test
    void invalidToken_returnsFalse() {
        JwtTokenProvider provider = provider();
        assertFalse(provider.validateToken("invalid.token.here"));
    }

    @Test
    void emptyToken_returnsFalse() {
        JwtTokenProvider provider = provider();
        assertFalse(provider.validateToken(""));
    }

    @Test
    void tokenFromDifferentKey_returnsFalse() {
        JwtTokenProvider provider1 = new JwtTokenProvider("secret-one", 3600000, 86400000);
        JwtTokenProvider provider2 = new JwtTokenProvider("secret-two", 3600000, 86400000);

        String token = provider1.generateAccessToken("u1", "user", "USER");
        assertFalse(provider2.validateToken(token));
    }

    @Test
    void blankSecretThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtTokenProvider("", 3600000, 86400000));
    }

    @Test
    void shortSecretIsHandledViaSha512() {
        JwtTokenProvider provider = new JwtTokenProvider("short", 3600000, 86400000);
        String token = provider.generateAccessToken("u1", "test", "USER");
        assertTrue(provider.validateToken(token));
        assertEquals("u1", provider.getUserIdFromToken(token));
    }

    @Test
    void longSecretWorks() {
        String longSecret = "a".repeat(128);
        JwtTokenProvider provider = new JwtTokenProvider(longSecret, 3600000, 86400000);
        String token = provider.generateAccessToken("u1", "test", "ADMIN");
        assertTrue(provider.validateToken(token));
    }

    @Test
    void expiredToken_returnsFalse() {
        // Create provider with 0ms expiration
        JwtTokenProvider provider = new JwtTokenProvider("test-secret-expired", 0, 0);
        String token = provider.generateAccessToken("u1", "test", "USER");
        // Token is immediately expired
        assertFalse(provider.validateToken(token));
    }

    @Test
    void accessTokenContainsUsernameAndRole() {
        JwtTokenProvider provider = provider();
        String token = provider.generateAccessToken("u1", "jdoe", "ADMIN");

        assertEquals("u1", provider.getUserIdFromToken(token));
        assertEquals("ADMIN", provider.getRoleFromToken(token));
    }
}
