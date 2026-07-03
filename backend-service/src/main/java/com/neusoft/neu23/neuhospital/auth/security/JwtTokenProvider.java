package com.neusoft.neu23.neuhospital.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Minimal JWT utility for issuing and validating access tokens.
 */
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_BIZ_ID = "bizId";
    private static final String CLAIM_SESSION_ID = "sessionId";
    private static final String CLAIM_REFRESH_TOKEN_ID = "refreshTokenId";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId,
                                      String username,
                                      String role,
                                      String userType,
                                      Long bizId,
                                      String sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.accessExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_USER_TYPE, userType)
                .claim(CLAIM_BIZ_ID, bizId)
                .claim(CLAIM_SESSION_ID, sessionId)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId,
                                       String role,
                                       String userType,
                                       Long bizId,
                                       String sessionId,
                                       String refreshTokenId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.refreshExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_USER_TYPE, userType)
                .claim(CLAIM_BIZ_ID, bizId)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_REFRESH_TOKEN_ID, refreshTokenId)
                .signWith(secretKey)
                .compact();
    }

    public JwtUserClaims parseAccessToken(String token) {
        return toUserClaims(parseClaims(token));
    }

    public JwtUserClaims parseRefreshToken(String token) {
        return toUserClaims(parseClaims(token));
    }

    public String extractRefreshTokenId(String token) {
        return parseClaims(token).get(CLAIM_REFRESH_TOKEN_ID, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getAccessExpirationSeconds() {
        return properties.accessExpirationSeconds();
    }

    public long getRefreshExpirationSeconds() {
        return properties.refreshExpirationSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtUserClaims toUserClaims(Claims claims) {
        Number bizIdValue = claims.get(CLAIM_BIZ_ID, Number.class);
        return new JwtUserClaims(
                Long.parseLong(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE, String.class),
                claims.get(CLAIM_USER_TYPE, String.class),
                bizIdValue == null ? null : bizIdValue.longValue(),
                claims.get(CLAIM_SESSION_ID, String.class),
                claims.getIssuer()
        );
    }
}
