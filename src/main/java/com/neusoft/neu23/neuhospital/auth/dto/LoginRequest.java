package com.neusoft.neu23.neuhospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Minimal login request payload.
 */
public record LoginRequest(
        @Schema(description = "登录用户名", example = "admin")
        @NotBlank(message = "用户名不能为空")
        String username,
        @Schema(description = "登录密码", example = "123456")
        @NotBlank(message = "密码不能为空")
        String password
) {
}
