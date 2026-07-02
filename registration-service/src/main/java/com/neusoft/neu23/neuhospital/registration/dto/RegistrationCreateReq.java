package com.neusoft.neu23.neuhospital.registration.dto;

import jakarta.validation.constraints.NotNull;

public class RegistrationCreateReq {
    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;

    private Long patientId;
    
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
}
