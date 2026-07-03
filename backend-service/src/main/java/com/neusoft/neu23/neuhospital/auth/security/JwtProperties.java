package com.neusoft.neu23.neuhospital.auth.security;

/**
 * Minimal JWT settings holder used by the token provider.
 */
public record JwtProperties(
        String secret,
        String issuer,
        long accessExpirationSeconds,
        long refreshExpirationSeconds
) {

    public JwtProperties(String secret, String issuer, long accessExpirationSeconds) {
        this(secret, issuer, accessExpirationSeconds, accessExpirationSeconds);
    }
}
