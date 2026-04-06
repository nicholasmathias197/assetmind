package com.assetmind.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void defaultValues() {
        UserEntity user = new UserEntity();
        assertEquals("USER", user.getRole());
        assertTrue(user.isEnabled());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void gettersAndSetters() {
        UserEntity user = new UserEntity();
        user.setId("u1");
        user.setUsername("john");
        user.setPassword("secret");
        user.setEmail("john@example.com");
        user.setRole("ADMIN");
        user.setEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);

        assertEquals("u1", user.getId());
        assertEquals("john", user.getUsername());
        assertEquals("secret", user.getPassword());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("ADMIN", user.getRole());
        assertFalse(user.isEnabled());
        assertEquals(now, user.getCreatedAt());
    }
}
