package com.neusoft.neu23.neuhospital.auth.vo;

import java.time.Instant;

/**
 * Minimal login response payload carrying the issued access token.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        Long userId,
        String username,
        String role,
        String userType,
        Long bizId,
        String sessionId,
        String refreshTokenId
) {
}
