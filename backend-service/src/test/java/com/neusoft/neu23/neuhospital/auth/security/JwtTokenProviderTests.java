package com.neusoft.neu23.neuhospital.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTests {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtProperties properties = new JwtProperties(
                "smart-medical-secret-key-for-tests-1234567890",
                "smart-medical",
                3600L
        );
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateAccessToken(1001L, "doctor001", "DOCTOR", "doctor", 30001L, "sess_test_001");
        JwtUserClaims claims = provider.parseAccessToken(token);

        assertNotNull(token);
        assertEquals(1001L, claims.userId());
        assertEquals("doctor001", claims.username());
        assertEquals("DOCTOR", claims.role());
        assertEquals("doctor", claims.userType());
        assertEquals(30001L, claims.bizId());
        assertEquals("sess_test_001", claims.sessionId());
        assertEquals("smart-medical", claims.issuer());
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtProperties properties = new JwtProperties(
                "smart-medical-secret-key-for-tests-1234567890",
                "smart-medical",
                3600L
        );
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertFalse(provider.isTokenValid("bad.token.value"));
    }
}
