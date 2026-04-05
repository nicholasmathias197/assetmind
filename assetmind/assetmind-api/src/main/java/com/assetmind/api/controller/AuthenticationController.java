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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final List<String> DEFAULT_FEATURE_ACCESS = List.of(
            "ASSETS",
            "DEPRECIATION",
            "TAX_STRATEGY",
            "CLASSIFICATION",
            "BREAKOUT"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final SpringDataUserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapSecret;

    public AuthenticationController(JwtTokenProvider jwtTokenProvider,
                                    SpringDataUserJpaRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    @org.springframework.beans.factory.annotation.Value("${assetmind.bootstrap-secret}") String bootstrapSecret) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapSecret = bootstrapSecret;
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

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            parseFeatureAccess(user.getFeatureAccess())
        );
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
        user.setFeatureAccess("");
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new Object() {
            public final String message = "User registered successfully";
            public final String userId = user.getId();
        });
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<LoginResponse> bootstrapAdmin(
            @Valid @RequestBody RegisterRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Bootstrap-Key") String bootstrapKey) {
        if (!bootstrapSecret.equals(bootstrapKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid bootstrap key");
        }
        if (userRepository.existsByRoleIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin already exists");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserEntity admin = new UserEntity();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(request.username());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setEmail(request.email());
        admin.setRole("ADMIN");
        admin.setFeatureAccess(String.join(",", DEFAULT_FEATURE_ACCESS));
        admin.setEnabled(true);

        userRepository.save(admin);

        String accessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(),
                admin.getUsername(),
                admin.getRole(),
                DEFAULT_FEATURE_ACCESS
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getId());

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            parseFeatureAccess(user.getFeatureAccess())
        );

        LoginResponse response = new LoginResponse(
                newAccessToken,
                request.refreshToken(),
                "Bearer",
                3600
        );

        return ResponseEntity.ok(response);
    }

    private List<String> parseFeatureAccess(String featureAccess) {
        if (featureAccess == null || featureAccess.isBlank()) {
            return List.of();
        }
        return Arrays.stream(featureAccess.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }
}

