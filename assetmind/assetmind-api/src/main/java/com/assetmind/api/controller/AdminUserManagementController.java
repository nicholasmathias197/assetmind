package com.assetmind.api.controller;

import com.assetmind.api.dto.AdminUserAccessUpdateRequest;
import com.assetmind.api.dto.AdminUserResponse;
import com.assetmind.infrastructure.security.SpringDataUserJpaRepository;
import com.assetmind.infrastructure.security.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private static final Set<String> ALLOWED_FEATURES = Set.of(
            "ASSETS",
            "DEPRECIATION",
            "TAX_STRATEGY",
            "CLASSIFICATION",
            "BREAKOUT"
    );

    private final SpringDataUserJpaRepository userRepository;

    public AdminUserManagementController(SpringDataUserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{userId}/access")
    public ResponseEntity<AdminUserResponse> updateUserAccess(
            @PathVariable String userId,
            @RequestBody AdminUserAccessUpdateRequest request
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.role() != null && !request.role().isBlank()) {
            String normalizedRole = request.role().trim().toUpperCase(Locale.ROOT);
            if (!normalizedRole.equals("ADMIN") && !normalizedRole.equals("USER")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be ADMIN or USER");
            }
            user.setRole(normalizedRole);
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.featureAccess() != null) {
            List<String> normalizedFeatures = normalizeFeatures(request.featureAccess());
            user.setFeatureAccess(String.join(",", normalizedFeatures));
        }

        userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                parseFeatures(user.getFeatureAccess()),
                user.getCreatedAt()
        );
    }

    private List<String> parseFeatures(String featureAccess) {
        if (featureAccess == null || featureAccess.isBlank()) {
            return List.of();
        }

        return Arrays.stream(featureAccess.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> normalizeFeatures(List<String> requestedFeatures) {
        LinkedHashSet<String> normalized = requestedFeatures.stream()
                .filter(feature -> feature != null && !feature.isBlank())
                .map(feature -> feature.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String feature : normalized) {
            if (!ALLOWED_FEATURES.contains(feature)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported feature: " + feature + ". Allowed: " + String.join(", ", ALLOWED_FEATURES)
                );
            }
        }

        return normalized.stream().toList();
    }
}
