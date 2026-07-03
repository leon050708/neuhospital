package com.neusoft.neu23.neuhospital.ai.dto;

public class ChatSessionCreateReq {
    private Long registrationId;
    private String sessionType;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
}
