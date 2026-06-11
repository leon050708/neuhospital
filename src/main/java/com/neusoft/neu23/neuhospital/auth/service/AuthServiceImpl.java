package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.dto.LoginRequest;
import com.neusoft.neu23.neuhospital.auth.dto.RefreshTokenRequest;
import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;
import com.neusoft.neu23.neuhospital.auth.security.JwtTokenProvider;
import com.neusoft.neu23.neuhospital.auth.security.JwtUserClaims;
import com.neusoft.neu23.neuhospital.auth.security.LoginSession;
import com.neusoft.neu23.neuhospital.auth.security.LoginSessionStore;
import com.neusoft.neu23.neuhospital.auth.security.RefreshSession;
import com.neusoft.neu23.neuhospital.auth.security.UserSessionBinding;
import com.neusoft.neu23.neuhospital.auth.vo.LoginResponse;
import com.neusoft.neu23.neuhospital.auth.vo.RefreshTokenResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal auth service implementing login, refresh and logout.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final AuthAccountProvider accountProvider;
    private final LoginSessionStore sessionStore;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthAccountProvider accountProvider,
                           LoginSessionStore sessionStore,
                           JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.accountProvider = accountProvider;
        this.sessionStore = sessionStore;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthAccount account = accountProvider.findByUsername(request.username());
        if (account == null || !STATUS_ENABLED.equals(account.status())) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        UserSessionBinding existingBinding = sessionStore.getUserSessionBinding(account.userId());
        if (existingBinding != null) {
            sessionStore.removeSession(existingBinding.sessionId());
            sessionStore.removeRefreshToken(existingBinding.refreshTokenId());
            sessionStore.removeUserSessionBinding(account.userId());
        }

        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        String refreshTokenId = "rt_" + UUID.randomUUID().toString().replace("-", "");
        String accessToken = tokenProvider.generateAccessToken(
                account.userId(), account.username(), account.role(), account.userType(), account.bizId(), sessionId
        );
        String refreshToken = tokenProvider.generateRefreshToken(
                account.userId(), account.role(), account.userType(), account.bizId(), sessionId, refreshTokenId
        );

        Instant now = Instant.now();
        sessionStore.saveSession(new LoginSession(
                sessionId, account.userId(), account.username(), account.userType(), account.role(),
                account.bizId(), refreshTokenId, STATUS_ONLINE, now
        ), tokenProvider.getAccessExpirationSeconds());
        sessionStore.saveRefreshToken(new RefreshSession(
                refreshTokenId, sessionId, account.userId(), account.userType(), account.role(),
                account.bizId(), STATUS_ACTIVE, now
        ), tokenProvider.getRefreshExpirationSeconds());
        sessionStore.bindUserSession(account.userId(), sessionId, refreshTokenId);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                now.plusSeconds(tokenProvider.getAccessExpirationSeconds()),
                now.plusSeconds(tokenProvider.getRefreshExpirationSeconds()),
                account.userId(),
                account.username(),
                account.role(),
                account.userType(),
                account.bizId(),
                sessionId,
                refreshTokenId
        );
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        JwtUserClaims claims = tokenProvider.parseRefreshToken(request.refreshToken());
        String refreshTokenId = tokenProvider.extractRefreshTokenId(request.refreshToken());
        RefreshSession refreshSession = sessionStore.getRefreshToken(refreshTokenId);
        if (refreshSession == null || !refreshSession.sessionId().equals(claims.sessionId())) {
            throw new IllegalArgumentException("刷新令牌已失效");
        }

        LoginSession loginSession = sessionStore.getSession(claims.sessionId());
        if (loginSession == null) {
            throw new IllegalArgumentException("登录会话已失效");
        }

        String accessToken = tokenProvider.generateAccessToken(
                claims.userId(), loginSession.username(), claims.role(), claims.userType(), claims.bizId(), claims.sessionId()
        );

        return new RefreshTokenResponse(
                accessToken,
                "Bearer",
                Instant.now().plusSeconds(tokenProvider.getAccessExpirationSeconds()),
                claims.sessionId()
        );
    }

    @Override
    public void logout(String accessToken) {
        JwtUserClaims claims = tokenProvider.parseAccessToken(accessToken);
        LoginSession session = sessionStore.getSession(claims.sessionId());
        if (session != null) {
            sessionStore.removeSession(session.sessionId());
            sessionStore.removeRefreshToken(session.refreshTokenId());
        }
        sessionStore.removeUserSessionBinding(claims.userId());
    }
}
