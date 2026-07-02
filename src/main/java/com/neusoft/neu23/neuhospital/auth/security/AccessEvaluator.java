package com.neusoft.neu23.neuhospital.auth.security;

import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.system.entity.SysUserEntity;
import com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessEvaluator")
public class AccessEvaluator {

    private final RegistrationMapper registrationMapper;
    private final SysUserMapper sysUserMapper;

    public AccessEvaluator(RegistrationMapper registrationMapper, SysUserMapper sysUserMapper) {
        this.registrationMapper = registrationMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public boolean isCurrentPatient(Long patientId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null
                && "PATIENT".equals(user.getUserType())
                && user.getBizId() != null
                && user.getBizId().equals(patientId);
    }

    public boolean isCurrentDoctor(Long doctorId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null
                && "DOCTOR".equals(user.getUserType())
                && user.getBizId() != null
                && user.getBizId().equals(doctorId);
    }

    public boolean isCurrentUser(Long userId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null && user.getUserId().equals(userId);
    }

    public boolean isCurrentPatientUser(Long userId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user == null || !"PATIENT".equals(user.getUserType())) {
            return false;
        }
        SysUserEntity targetUser = sysUserMapper.selectById(userId);
        return targetUser != null
                && targetUser.getBizId() != null
                && user.getBizId() != null
                && targetUser.getBizId().equals(user.getBizId());
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
