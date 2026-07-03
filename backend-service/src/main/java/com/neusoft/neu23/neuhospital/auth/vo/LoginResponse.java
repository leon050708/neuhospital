package com.neusoft.neu23.neuhospital.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Minimal login response payload carrying the issued access token.
 */
public record LoginResponse(
        @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9.access")
        String accessToken,
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
        String refreshToken,
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "accessToken 过期时间")
        Instant accessTokenExpiresAt,
        @Schema(description = "refreshToken 过期时间")
        Instant refreshTokenExpiresAt,
        @Schema(description = "用户 ID", example = "1")
        Long userId,
        @Schema(description = "用户名", example = "admin")
        String username,
        @Schema(description = "角色编码", example = "ADMIN")
        String role,
        @Schema(description = "用户类型", example = "SYSTEM")
        String userType,
        @Schema(description = "业务实体 ID", example = "1001")
        Long bizId,
        @Schema(description = "登录会话 ID")
        String sessionId,
        @Schema(description = "刷新令牌 ID")
        String refreshTokenId
) {
}
