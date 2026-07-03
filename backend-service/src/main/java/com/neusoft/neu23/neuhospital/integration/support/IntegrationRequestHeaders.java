package com.neusoft.neu23.neuhospital.integration.support;

import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class IntegrationRequestHeaders {

    public void apply(HttpHeaders headers) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return;
        }
        headers.set("X-User-Id", String.valueOf(user.getUserId()));
        headers.set("X-Username", user.getUsername());
        headers.set("X-User-Roles", extractRole(user));
        headers.set("X-User-Type", user.getUserType());
        if (user.getBizId() != null) {
            headers.set("X-Biz-Id", String.valueOf(user.getBizId()));
        }
    }

    private String extractRole(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("");
    }
}
