package com.assetmind.api.dto;

import java.util.List;

public record AdminUserAccessUpdateRequest(
        String role,
        Boolean enabled,
        List<String> featureAccess
) {
}
