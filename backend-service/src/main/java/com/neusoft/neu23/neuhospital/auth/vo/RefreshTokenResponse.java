package com.neusoft.neu23.neuhospital.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response payload after a successful access-token refresh.
 */
public record RefreshTokenResponse(
        @Schema(description = "新的 accessToken")
        String accessToken,
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "accessToken 过期时间")
        Instant accessTokenExpiresAt,
        @Schema(description = "登录会话 ID")
        String sessionId
) {
}
