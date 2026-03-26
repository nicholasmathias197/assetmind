package com.assetmind.api.controller;

import com.assetmind.api.dto.LoginRequest;
import com.assetmind.api.dto.LoginResponse;
import com.assetmind.api.dto.RegisterRequest;
import com.assetmind.api.dto.RefreshTokenRequest;
import com.assetmind.infrastructure.security.JwtTokenProvider;
import com.assetmind.infrastructure.security.SpringDataUserJpaRepository;
import com.assetmind.infrastructure.security.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final JwtTokenProvider jwtTokenProvider;
    private final SpringDataUserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(JwtTokenProvider jwtTokenProvider,
                                    SpringDataUserJpaRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600 // 1 hour in seconds
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole("USER");
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new Object() {
            public final String message = "User registered successfully";
            public final String userId = user.getId();
        });
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());

        LoginResponse response = new LoginResponse(
                newAccessToken,
                request.refreshToken(),
                "Bearer",
                3600
        );

        return ResponseEntity.ok(response);
    }
}

