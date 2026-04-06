package com.assetmind.api.controller;

import com.assetmind.api.dto.LoginRequest;
import com.assetmind.api.dto.LoginResponse;
import com.assetmind.api.dto.RefreshTokenRequest;
import com.assetmind.api.dto.RegisterRequest;
import com.assetmind.infrastructure.security.JwtTokenProvider;
import com.assetmind.infrastructure.security.SpringDataUserJpaRepository;
import com.assetmind.infrastructure.security.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {

    private JwtTokenProvider jwtTokenProvider;
    private SpringDataUserJpaRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        userRepository = mock(SpringDataUserJpaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new AuthenticationController(jwtTokenProvider, userRepository, passwordEncoder);
    }

    private UserEntity enabledUser() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setUsername("john");
        user.setPassword("encoded-pass");
        user.setRole("USER");
        user.setEnabled(true);
        return user;
    }

    @Test
    void loginSuccessReturnsTokens() {
        UserEntity user = enabledUser();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "encoded-pass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("user-1", "john", "USER")).thenReturn("access-tok");
        when(jwtTokenProvider.generateRefreshToken("user-1")).thenReturn("refresh-tok");

        ResponseEntity<LoginResponse> response = controller.login(new LoginRequest("john", "pass123"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("access-tok", response.getBody().accessToken());
        assertEquals("refresh-tok", response.getBody().refreshToken());
        assertEquals("Bearer", response.getBody().tokenType());
        assertEquals(3600, response.getBody().expiresIn());
    }

    @Test
    void loginFailsWithWrongUsername() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new LoginRequest("unknown", "pass")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void loginFailsWithWrongPassword() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(enabledUser()));
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new LoginRequest("john", "wrong")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void loginFailsWhenUserDisabled() {
        UserEntity user = enabledUser();
        user.setEnabled(false);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "encoded-pass")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new LoginRequest("john", "pass123")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void registerSuccessCreatesUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        ResponseEntity<?> response = controller.register(
                new RegisterRequest("newuser", "password123", "new@test.com"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerFailsWhenUsernameExists() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(enabledUser()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.register(new RegisterRequest("john", "password123", "john@test.com")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void refreshTokenReturnsNewAccessToken() {
        when(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh")).thenReturn("user-1");
        UserEntity user = enabledUser();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken("user-1", "john", "USER")).thenReturn("new-access");

        ResponseEntity<LoginResponse> response = controller.refreshToken(
                new RefreshTokenRequest("valid-refresh"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("new-access", response.getBody().accessToken());
        assertEquals("valid-refresh", response.getBody().refreshToken());
    }

    @Test
    void refreshTokenFailsWithInvalidToken() {
        when(jwtTokenProvider.validateToken("bad-refresh")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.refreshToken(new RefreshTokenRequest("bad-refresh")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refreshTokenFailsWhenUserNotFound() {
        when(jwtTokenProvider.validateToken("orphan-refresh")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("orphan-refresh")).thenReturn("deleted-user");
        when(userRepository.findById("deleted-user")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.refreshToken(new RefreshTokenRequest("orphan-refresh")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
