package com.neusoft.neu23.neuhospital.ai.dto;

import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;

import java.time.LocalDateTime;

public class ChatSessionResp {

    private String sessionNo;
    private Long registrationId;
    private String sessionType;
    private String status;
    private LocalDateTime startedAt;

    public static ChatSessionResp from(AiChatSessionEntity session) {
        ChatSessionResp resp = new ChatSessionResp();
        resp.setSessionNo(session.getSessionNo());
        resp.setRegistrationId(session.getRegistrationId());
        resp.setSessionType(session.getSessionType());
        resp.setStatus(session.getStatus());
        resp.setStartedAt(session.getStartedAt());
        return resp;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
