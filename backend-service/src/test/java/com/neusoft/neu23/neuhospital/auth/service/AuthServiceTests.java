package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.dto.LoginRequest;
import com.neusoft.neu23.neuhospital.auth.dto.RefreshTokenRequest;
import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;
import com.neusoft.neu23.neuhospital.auth.security.InMemoryLoginSessionStore;
import com.neusoft.neu23.neuhospital.auth.security.JwtProperties;
import com.neusoft.neu23.neuhospital.auth.security.JwtTokenProvider;
import com.neusoft.neu23.neuhospital.auth.vo.LoginResponse;
import com.neusoft.neu23.neuhospital.auth.vo.RefreshTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTests {

    @Test
    void loginShouldCreateSessionAndTokens() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthAccountProvider accountProvider = username -> {
            if (!"doctor001".equals(username)) {
                return null;
            }
            return new AuthAccount(
                    101L,
                    "doctor001",
                    passwordEncoder.encode("password123"),
                    "DOCTOR",
                    "DOCTOR",
                    30001L,
                    "ENABLED"
            );
        };
        InMemoryLoginSessionStore sessionStore = new InMemoryLoginSessionStore();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(new JwtProperties(
                "smart-medical-secret-key-for-tests-1234567890",
                "smart-medical",
                3600L,
                604800L
        ));
        AuthService service = new AuthServiceImpl(accountProvider, sessionStore, tokenProvider, passwordEncoder);

        LoginResponse response = service.login(new LoginRequest("doctor001", "password123"));

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("DOCTOR", response.userType());
        assertEquals("DOCTOR", response.role());
        assertEquals(30001L, response.bizId());
        assertNotNull(sessionStore.getSession(response.sessionId()));
        assertNotNull(sessionStore.getRefreshToken(response.refreshTokenId()));
    }

    @Test
    void refreshShouldIssueNewAccessTokenForActiveRefreshToken() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthAccountProvider accountProvider = username -> new AuthAccount(
                101L,
                "doctor001",
                passwordEncoder.encode("password123"),
                "DOCTOR",
                "DOCTOR",
                30001L,
                "ENABLED"
        );
        InMemoryLoginSessionStore sessionStore = new InMemoryLoginSessionStore();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(new JwtProperties(
                "smart-medical-secret-key-for-tests-1234567890",
                "smart-medical",
                3600L,
                604800L
        ));
        AuthService service = new AuthServiceImpl(accountProvider, sessionStore, tokenProvider, passwordEncoder);

        LoginResponse loginResponse = service.login(new LoginRequest("doctor001", "password123"));

        RefreshTokenResponse refreshResponse = service.refreshToken(new RefreshTokenRequest(loginResponse.refreshToken()));

        assertNotNull(refreshResponse.accessToken());
        assertEquals("Bearer", refreshResponse.tokenType());
        assertEquals(loginResponse.sessionId(), refreshResponse.sessionId());
    }

    @Test
    void logoutShouldInvalidateSessionAndRefreshToken() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthAccountProvider accountProvider = username -> new AuthAccount(
                201L,
                "patient001",
                passwordEncoder.encode("password123"),
                "PATIENT",
                "PATIENT",
                20001L,
                "ENABLED"
        );
        InMemoryLoginSessionStore sessionStore = new InMemoryLoginSessionStore();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(new JwtProperties(
                "smart-medical-secret-key-for-tests-1234567890",
                "smart-medical",
                3600L,
                604800L
        ));
        AuthService service = new AuthServiceImpl(accountProvider, sessionStore, tokenProvider, passwordEncoder);

        LoginResponse loginResponse = service.login(new LoginRequest("patient001", "password123"));
        service.logout(loginResponse.accessToken());

        assertNull(sessionStore.getSession(loginResponse.sessionId()));
        assertNull(sessionStore.getRefreshToken(loginResponse.refreshTokenId()));
    }
}
