package com.assetmind.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetmindUserDetailsServiceTest {

    @Test
    void loadUserByUsername_found() {
        SpringDataUserJpaRepository repo = mock(SpringDataUserJpaRepository.class);
        UserEntity entity = new UserEntity();
        entity.setId("u1");
        entity.setUsername("jdoe");
        entity.setPassword("hashed");
        entity.setRole("USER");
        entity.setEnabled(true);

        when(repo.findByUsername("jdoe")).thenReturn(Optional.of(entity));

        AssetmindUserDetailsService service = new AssetmindUserDetailsService(repo);
        UserDetails details = service.loadUserByUsername("jdoe");

        assertEquals("jdoe", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertTrue(details.isEnabled());
    }

    @Test
    void loadUserByUsername_notFound() {
        SpringDataUserJpaRepository repo = mock(SpringDataUserJpaRepository.class);
        when(repo.findByUsername("unknown")).thenReturn(Optional.empty());

        AssetmindUserDetailsService service = new AssetmindUserDetailsService(repo);

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown"));
    }
}
