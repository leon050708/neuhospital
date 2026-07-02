package com.neusoft.neu23.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.neusoft.neu23.gateway.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_BIZ_ID = "bizId";
    private static final String CLAIM_SESSION_ID = "sessionId";

    private static final List<String> WHITE_LIST_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars",
            "/actuator"
    );

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public AuthGlobalFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String traceId = resolveTraceId(exchange);

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isWhiteListed(path)) {
            return chain.filter(exchange.mutate().request(addTraceHeaders(exchange.getRequest(), traceId)).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        try {
            Claims claims = parseClaims(authHeader.substring(7));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-Username");
                        headers.remove("X-User-Roles");
                        headers.remove("X-User-Type");
                        headers.remove("X-Biz-Id");
                        headers.remove("X-Session-Id");
                        headers.remove("X-Trace-Id");
                        headers.set("X-User-Id", claims.getSubject());
                        headers.set("X-Username", stringClaim(claims, CLAIM_USERNAME));
                        headers.set("X-User-Roles", stringClaim(claims, CLAIM_ROLE));
                        headers.set("X-User-Type", stringClaim(claims, CLAIM_USER_TYPE));
                        headers.set("X-Session-Id", stringClaim(claims, CLAIM_SESSION_ID));
                        headers.set("X-Trace-Id", traceId);
                        Number bizId = claims.get(CLAIM_BIZ_ID, Number.class);
                        if (bizId != null) {
                            headers.set("X-Biz-Id", String.valueOf(bizId.longValue()));
                        }
                    })
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, "Invalid bearer token");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getSubject() == null) {
            throw new JwtException("JWT subject is missing");
        }
        return claims;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString().replace("-", "") : traceId;
    }

    private ServerHttpRequest addTraceHeaders(ServerHttpRequest request, String traceId) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove("X-Trace-Id");
                    headers.set("X-Trace-Id", traceId);
                })
                .build();
    }

    private String stringClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        return value == null ? "" : value;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":401,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
