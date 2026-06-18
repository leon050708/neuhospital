package com.neusoft.neu23.neuhospital.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnBizIdForCurrentPatient() {
        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertEquals(35L, SecurityUtils.getCurrentPatientId());
    }

    @Test
    void shouldRejectNonPatientWhenResolvingCurrentPatient() {
        CustomUserDetails principal = new CustomUserDetails(18L, "dr_li", "DOCTOR", "DOCTOR", 8L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentPatientId);
    }
}
