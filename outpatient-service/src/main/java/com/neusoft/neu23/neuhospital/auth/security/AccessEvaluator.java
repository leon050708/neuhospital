package com.neusoft.neu23.neuhospital.auth.security;

import com.neusoft.neu23.neuhospital.outpatient.entity.MedicalRecordEntity;
import com.neusoft.neu23.neuhospital.outpatient.mapper.MedicalRecordMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessEvaluator")
public class AccessEvaluator {

    private final MedicalRecordMapper medicalRecordMapper;

    public AccessEvaluator(MedicalRecordMapper medicalRecordMapper) {
        this.medicalRecordMapper = medicalRecordMapper;
    }

    public boolean isCurrentPatient(Long patientId) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        return user != null
                && "PATIENT".equals(user.getUserType())
                && user.getBizId() != null
                && user.getBizId().equals(patientId);
    }

    public boolean canAccessMedicalRecord(Long recordId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                || authentication.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGEMENT".equals(a.getAuthority()))
                || authentication.getAuthorities().stream().anyMatch(a -> "ROLE_DOCTOR".equals(a.getAuthority()))) {
            return true;
        }
        MedicalRecordEntity record = medicalRecordMapper.selectById(recordId);
        if (record == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails user) {
            return "PATIENT".equals(user.getUserType())
                    && user.getBizId() != null
                    && user.getBizId().equals(record.getPatientId());
        }
        return false;
    }
}
