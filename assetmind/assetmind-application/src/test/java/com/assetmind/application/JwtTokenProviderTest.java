package com.assetmind.application;

import com.assetmind.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void generatesAndValidatesTokenWhenConfiguredSecretIsShort() {
        JwtTokenProvider provider = new JwtTokenProvider("short-local-secret", 3_600_000, 86_400_000);

        String token = provider.generateAccessToken("user-123", "john_doe", "USER", List.of("ASSETS"));

        assertTrue(provider.validateToken(token));
        assertEquals("user-123", provider.getUserIdFromToken(token));
        assertEquals("USER", provider.getRoleFromToken(token));
        assertEquals(List.of("ASSETS"), provider.getFeatureAccessFromToken(token));
    }

    @Test
    void rejectsBlankSecret() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtTokenProvider("   ", 3_600_000, 86_400_000)
        );

        assertEquals("jwt.secret must not be blank", exception.getMessage());
    }
}

