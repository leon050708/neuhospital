package com.neusoft.neu23.neuhospital.auth.security;

import java.time.Instant;

/**
 * Server-side login session metadata stored in Redis or test doubles.
 */
public record LoginSession(
        String sessionId,
        Long userId,
        String username,
        String userType,
        String role,
        Long bizId,
        String refreshTokenId,
        String status,
        Instant loginTime
) {
}
