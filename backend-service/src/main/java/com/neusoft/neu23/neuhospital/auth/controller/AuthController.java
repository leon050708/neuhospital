package com.neusoft.neu23.neuhospital.auth.controller;

import com.neusoft.neu23.neuhospital.auth.dto.LoginRequest;
import com.neusoft.neu23.neuhospital.auth.dto.RefreshTokenRequest;
import com.neusoft.neu23.neuhospital.auth.service.AuthService;
import com.neusoft.neu23.neuhospital.auth.vo.LoginResponse;
import com.neusoft.neu23.neuhospital.auth.vo.RefreshTokenResponse;
import com.neusoft.neu23.neuhospital.auth.vo.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal auth API exposing login, refresh and logout.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "登录、令牌刷新、退出登录与当前用户信息接口")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "账号登录",
            description = "使用用户名和密码换取 accessToken 与 refreshToken。",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "刷新 accessToken",
            description = "使用 refreshToken 换取新的 accessToken。",
            security = {}
    )
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "让当前 accessToken 对应的登录会话失效。")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Bearer Token，例如：Bearer eyJhbGciOi...", required = true)
            @RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : authorization;
        authService.logout(accessToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @Operation(
            summary = "注册患者账号",
            description = "创建新的患者端登录账号。该接口当前是否公开，以安全配置为准。",
            security = {}
    )
    public ResponseEntity<Void> register(@RequestBody com.neusoft.neu23.neuhospital.auth.dto.RegisterReq request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户", description = "根据当前 Bearer Token 返回用户资料。")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserProfileResponse> me(
            @Parameter(description = "Bearer Token，例如：Bearer eyJhbGciOi...", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.status(401).build();
        }
        String accessToken = authorization.substring(BEARER_PREFIX.length());
        return ResponseEntity.ok(authService.getCurrentUser(accessToken));
    }
}
