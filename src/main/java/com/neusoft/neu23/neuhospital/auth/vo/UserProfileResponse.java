package com.neusoft.neu23.neuhospital.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Minimal user profile response for /api/auth/me
 */
public record UserProfileResponse(
        @Schema(description = "用户 ID", example = "1")
        Long userId,
        @Schema(description = "用户名", example = "admin")
        String username,
        @Schema(description = "角色编码", example = "ADMIN")
        String role,
        @Schema(description = "用户类型", example = "SYSTEM")
        String userType,
        @Schema(description = "业务实体 ID", example = "1001")
        Long bizId
) {
}
