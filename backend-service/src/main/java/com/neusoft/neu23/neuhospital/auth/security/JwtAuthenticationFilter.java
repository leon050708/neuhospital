package com.neusoft.neu23.neuhospital.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionStore loginSessionStore;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, LoginSessionStore loginSessionStore) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginSessionStore = loginSessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenProvider.isTokenValid(token)) {
                JwtUserClaims claims = jwtTokenProvider.parseAccessToken(token);
                
                // Verify session exists and is valid in store
                LoginSession session = loginSessionStore.getSession(claims.sessionId());
                if (session != null) {
                    CustomUserDetails userDetails = new CustomUserDetails(
                            claims.userId(),
                            claims.username(),
                            claims.role(),
                            claims.userType(),
                            claims.bizId()
                    );

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
