package com.assetmind.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        String role,
        boolean enabled,
        List<String> featureAccess,
        LocalDateTime createdAt
) {
}
