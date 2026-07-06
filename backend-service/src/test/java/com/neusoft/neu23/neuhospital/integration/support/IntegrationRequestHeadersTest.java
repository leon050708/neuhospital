package com.neusoft.neu23.neuhospital.integration.support;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationRequestHeadersTest {

    private final IntegrationRequestHeaders requestHeaders = new IntegrationRequestHeaders();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiAgentSessionContext.clear();
    }

    @Test
    void shouldUseSecurityContextHeadersWhenAuthenticated() {
        CustomUserDetails user = new CustomUserDetails(101L, "patient01", "PATIENT", "PATIENT", 9301L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        HttpHeaders headers = new HttpHeaders();
        requestHeaders.apply(headers);

        assertEquals("101", headers.getFirst("X-User-Id"));
        assertEquals("patient01", headers.getFirst("X-Username"));
        assertEquals("PATIENT", headers.getFirst("X-User-Roles"));
        assertEquals("PATIENT", headers.getFirst("X-User-Type"));
        assertEquals("9301", headers.getFirst("X-Biz-Id"));
    }

    @Test
    void shouldFallbackToAiPatientContextWhenSecurityContextMissing() {
        AiAgentSessionContext.bind(88L, 9301L, null);

        HttpHeaders headers = new HttpHeaders();
        requestHeaders.apply(headers);

        assertEquals("9301", headers.getFirst("X-User-Id"));
        assertEquals("ai-patient-9301", headers.getFirst("X-Username"));
        assertEquals("PATIENT", headers.getFirst("X-User-Roles"));
        assertEquals("PATIENT", headers.getFirst("X-User-Type"));
        assertEquals("9301", headers.getFirst("X-Biz-Id"));
        assertEquals("88", headers.getFirst("X-Session-Id"));
    }
}
