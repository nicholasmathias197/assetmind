package com.assetmind.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class AssetmindUserDetailsTest {

    private UserEntity createUser(String id, String username, String password, String role, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void gettersReturnCorrectValues() {
        UserEntity user = createUser("u1", "jdoe", "hashed", "ADMIN", true);
        AssetmindUserDetails details = new AssetmindUserDetails(user);

        assertEquals("u1", details.getId());
        assertEquals("jdoe", details.getUsername());
        assertEquals("hashed", details.getPassword());
        assertEquals("ADMIN", details.getRole());
        assertTrue(details.isEnabled());
    }

    @Test
    void authoritiesContainRolePrefix() {
        UserEntity user = createUser("u1", "jdoe", "hashed", "USER", true);
        AssetmindUserDetails details = new AssetmindUserDetails(user);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void adminRoleAuthority() {
        UserEntity user = createUser("u2", "admin", "hashed", "ADMIN", true);
        AssetmindUserDetails details = new AssetmindUserDetails(user);

        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void accountStatusMethodsReturnTrue() {
        UserEntity user = createUser("u1", "jdoe", "hashed", "USER", true);
        AssetmindUserDetails details = new AssetmindUserDetails(user);

        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void disabledUser() {
        UserEntity user = createUser("u1", "jdoe", "hashed", "USER", false);
        AssetmindUserDetails details = new AssetmindUserDetails(user);

        assertFalse(details.isEnabled());
    }
}
