package com.neusoft.neu23.neuhospital.auth.security;

/**
 * Parsed identity data extracted from a JWT access token.
 */
public record JwtUserClaims(
        long userId,
        String username,
        String role,
        String userType,
        Long bizId,
        String sessionId,
        String issuer
) {
}
