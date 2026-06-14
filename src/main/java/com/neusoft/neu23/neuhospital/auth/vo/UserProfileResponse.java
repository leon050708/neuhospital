package com.neusoft.neu23.neuhospital.auth.vo;

/**
 * Minimal user profile response for /api/auth/me
 */
public record UserProfileResponse(
        Long userId,
        String username,
        String role,
        String userType,
        Long bizId
) {
}
