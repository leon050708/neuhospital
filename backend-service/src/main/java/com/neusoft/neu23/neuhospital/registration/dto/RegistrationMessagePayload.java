package com.neusoft.neu23.neuhospital.registration.dto;

public class RegistrationMessagePayload {
    private String msgId;
    private Long scheduleId;
    private Long patientId;

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
}
