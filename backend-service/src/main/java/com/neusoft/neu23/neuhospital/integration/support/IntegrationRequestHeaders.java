package com.neusoft.neu23.neuhospital.integration.support;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class IntegrationRequestHeaders {

    public void apply(HttpHeaders headers) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user != null) {
            headers.set("X-User-Id", String.valueOf(user.getUserId()));
            headers.set("X-Username", user.getUsername());
            headers.set("X-User-Roles", extractRole(user));
            headers.set("X-User-Type", user.getUserType());
            if (user.getBizId() != null) {
                headers.set("X-Biz-Id", String.valueOf(user.getBizId()));
            }
            return;
        }

        Long patientId = AiAgentSessionContext.getPatientId();
        Long sessionId = AiAgentSessionContext.getSessionId();
        if (patientId != null) {
            // AI 工具调用链可能不在原始 Web 线程里，此时回退到会话绑定的患者身份。
            headers.set("X-User-Id", String.valueOf(patientId));
            headers.set("X-Username", "ai-patient-" + patientId);
            headers.set("X-User-Roles", "PATIENT");
            headers.set("X-User-Type", "PATIENT");
            headers.set("X-Biz-Id", String.valueOf(patientId));
            if (sessionId != null) {
                headers.set("X-Session-Id", String.valueOf(sessionId));
            }
        }
    }

    private String extractRole(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("");
    }
}
