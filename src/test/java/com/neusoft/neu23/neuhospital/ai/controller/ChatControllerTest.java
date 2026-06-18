package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.agent.ChatAgentService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.dto.ChatSessionCreateReq;
import com.neusoft.neu23.neuhospital.ai.dto.ChatMessageReq;
import com.neusoft.neu23.neuhospital.ai.dto.ChatSessionResp;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import com.neusoft.neu23.neuhospital.common.response.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class ChatControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateSessionUsingCurrentPatientBizId() {
        ChatAgentService chatAgentService = mock(ChatAgentService.class);
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        ChatController controller = new ChatController(chatAgentService, sessionService);

        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setId(101L);
        session.setSessionNo("CHAT1234567890ABCDEF1234567890ABCD");
        when(chatAgentService.createSession(35L, 88L, "TRIAGE")).thenReturn(session);

        ChatSessionCreateReq req = new ChatSessionCreateReq();
        req.setRegistrationId(88L);
        req.setSessionType("TRIAGE");

        Result<ChatSessionResp> result = controller.createSession(req);

        ArgumentCaptor<Long> patientIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(chatAgentService).createSession(patientIdCaptor.capture(), org.mockito.Mockito.eq(88L), org.mockito.Mockito.eq("TRIAGE"));
        assertEquals(35L, patientIdCaptor.getValue());
        assertEquals(200, result.getCode());
        assertEquals("CHAT1234567890ABCDEF1234567890ABCD", result.getData().getSessionNo());
    }

    @Test
    void shouldRejectSessionCreationWhenRegistrationDoesNotBelongToCurrentPatient() {
        ChatAgentService chatAgentService = mock(ChatAgentService.class);
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        ChatController controller = new ChatController(chatAgentService, sessionService);

        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        doThrow(new IllegalArgumentException("挂号记录不存在或无权使用"))
                .when(chatAgentService).createSession(35L, 88L, "TRIAGE");

        ChatSessionCreateReq req = new ChatSessionCreateReq();
        req.setRegistrationId(88L);
        req.setSessionType("TRIAGE");

        Result<ChatSessionResp> result = controller.createSession(req);

        assertEquals(404, result.getCode());
        assertEquals("挂号记录不存在或无权使用", result.getMessage());
    }

    @Test
    void shouldRejectMessageWhenPatientAccessesOthersSession() {
        ChatAgentService chatAgentService = mock(ChatAgentService.class);
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        ChatController controller = new ChatController(chatAgentService, sessionService);

        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        doThrow(new IllegalArgumentException("会话不存在或无权访问"))
                .when(chatAgentService).chat("CHAT1234567890ABCDEF1234567890ABCD", 35L, "继续问诊");

        ChatMessageReq req = new ChatMessageReq();
        req.setContent("继续问诊");

        Result<String> result = controller.sendMessage("CHAT1234567890ABCDEF1234567890ABCD", req);

        assertEquals(404, result.getCode());
        assertEquals("会话不存在或无权访问", result.getMessage());
    }
}
