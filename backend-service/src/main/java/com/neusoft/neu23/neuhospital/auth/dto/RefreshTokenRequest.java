package com.neusoft.neu23.neuhospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Refresh-token request payload.
 */
public record RefreshTokenRequest(
        @Schema(description = "登录接口返回的 refreshToken", example = "eyJhbGciOiJIUzI1NiJ9.refresh")
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken
) {
}
