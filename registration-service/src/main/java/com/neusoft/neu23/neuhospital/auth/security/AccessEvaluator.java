package com.neusoft.neu23.neuhospital.auth.security;

import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessEvaluator")
public class AccessEvaluator {

    private final RegistrationMapper registrationMapper;

    public AccessEvaluator(RegistrationMapper registrationMapper) {
        this.registrationMapper = registrationMapper;
    }

    public boolean isCurrentPatient(Long patientId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null
                && "PATIENT".equals(user.getUserType())
                && user.getBizId() != null
                && user.getBizId().equals(patientId);
    }

    public boolean canAccessRegistration(Long registrationId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                || authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGEMENT".equals(a.getAuthority()))
                || authentication.getAuthorities().stream().anyMatch(a -> "ROLE_REGISTRATION_CLERK".equals(a.getAuthority()))) {
            return true;
        }
        RegistrationEntity registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails user) {
            if ("PATIENT".equals(user.getUserType()) && user.getBizId() != null) {
                return user.getBizId().equals(registration.getPatientId());
            }
            if ("DOCTOR".equals(user.getUserType()) && user.getBizId() != null) {
                return user.getBizId().equals(registration.getDoctorId());
            }
        }
        return false;
    }
}
