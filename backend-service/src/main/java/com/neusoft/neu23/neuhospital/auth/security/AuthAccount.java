package com.neusoft.neu23.neuhospital.auth.security;

/**
 * Minimal authenticated account view used by the auth module.
 */
public record AuthAccount(
        Long userId,
        String username,
        String passwordHash,
        String userType,
        String role,
        Long bizId,
        String status
) {
}
