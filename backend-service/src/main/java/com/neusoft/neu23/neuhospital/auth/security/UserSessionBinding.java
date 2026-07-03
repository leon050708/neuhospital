package com.neusoft.neu23.neuhospital.auth.security;

/**
 * Tracks the latest active session for one authenticated user.
 */
public record UserSessionBinding(
        Long userId,
        String sessionId,
        String refreshTokenId
) {
}
