package com.neusoft.neu23.neuhospital.auth.config;

import com.neusoft.neu23.neuhospital.auth.security.JwtProperties;
import com.neusoft.neu23.neuhospital.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auth-module beans used before full Spring Security integration is added.
 */
@Configuration
public class AuthConfig {

    @Bean
    public JwtProperties jwtProperties(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.access-expiration-seconds}") long accessExpirationSeconds,
            @Value("${auth.jwt.refresh-expiration-seconds}") long refreshExpirationSeconds
    ) {
        return new JwtProperties(secret, issuer, accessExpirationSeconds, refreshExpirationSeconds);
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
