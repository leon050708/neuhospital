package com.neusoft.neu23.neuhospital.auth.security;

import org.springframework.stereotype.Component;

@Component("accessEvaluator")
public class AccessEvaluator {

    public boolean isCurrentPatient(Long patientId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null
                && "PATIENT".equals(user.getUserType())
                && user.getBizId() != null
                && user.getBizId().equals(patientId);
    }
}
