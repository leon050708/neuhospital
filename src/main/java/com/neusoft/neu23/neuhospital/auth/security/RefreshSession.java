package com.neusoft.neu23.neuhospital.auth.security;

import java.time.Instant;

/**
 * Refresh-token metadata stored server-side.
 */
public record RefreshSession(
        String refreshTokenId,
        String sessionId,
        Long userId,
        String userType,
        String role,
        Long bizId,
        String status,
        Instant loginTime
) {
}
