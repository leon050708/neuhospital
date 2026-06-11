package com.neusoft.neu23.neuhospital.auth.vo;

import java.time.Instant;

/**
 * Response payload after a successful access-token refresh.
 */
public record RefreshTokenResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String sessionId
) {
}
