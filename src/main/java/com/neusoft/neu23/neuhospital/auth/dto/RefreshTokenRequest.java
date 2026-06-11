package com.neusoft.neu23.neuhospital.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh-token request payload.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken
) {
}
