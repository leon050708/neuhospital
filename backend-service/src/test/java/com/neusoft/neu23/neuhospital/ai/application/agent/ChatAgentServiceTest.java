package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAgentServiceTest {

    @Test
    void shouldRejectChatWhenSessionDoesNotBelongToCurrentPatient() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);

        assertThrows(IllegalArgumentException.class, () -> service.chat("CHAT1234567890ABCDEF1234567890ABCD", 35L, "我头疼"));
    }

    @Test
    void shouldRejectSessionCreationWhenRegistrationBelongsToAnotherPatient() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        RegistrationEntity registration = new RegistrationEntity();
        registration.setId(88L);
        registration.setPatientId(99L);
        org.mockito.Mockito.when(registrationService.getRegistrationById(88L)).thenReturn(registration);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);

        assertThrows(IllegalArgumentException.class, () -> service.createSession(35L, 88L, "TRIAGE"));
    }

    @Test
    void shouldGenerateNonEnumerableSessionNoWhenCreatingSession() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);

        service.createSession(35L, null, "TRIAGE");

        ArgumentCaptor<AiChatSessionEntity> captor = ArgumentCaptor.forClass(AiChatSessionEntity.class);
        verify(sessionService).save(captor.capture());
        String sessionNo = captor.getValue().getSessionNo();
        assertNotNull(sessionNo);
        assertTrue(sessionNo.matches("CHAT[a-f0-9]{32}"));
    }

    @Test
    void shouldInjectToolSafetyConstraintIntoPromptMessages() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);
        ReflectionTestUtils.setField(service, "systemPromptResource",
                new ByteArrayResource(("你是患者助诊助手，患者ID={patientId}。" +
                        "当前医院时区：{hospitalTimeZone}。" +
                        "当前北京时间日期：{currentDate}。" +
                        "当前北京时间星期：{currentWeekday}。" +
                        "当前北京时间：{currentDateTime}。").getBytes(StandardCharsets.UTF_8)));

        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setPatientId(35L);
        PatientAgentRoutingDecision decision = new PatientAgentRoutingDecision(false, false, false, "CLEAR", List.of(), List.of(), null, List.of());
        PatientAgentRagContext ragContext = new PatientAgentRagContext(decision, List.of(), "[医院知识证据]\n无");

        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) ReflectionTestUtils.invokeMethod(
                service,
                "buildPromptMessages",
                session,
                List.of(),
                ragContext
        );

        assertNotNull(messages);
        assertTrue(messages.stream().anyMatch(message -> message.getText().contains("Asia/Shanghai")));
        assertTrue(messages.stream().anyMatch(message -> message.getText().contains("当前北京时间日期")));
        assertTrue(messages.stream().anyMatch(message -> message.getText().matches("(?s).*星期[一二三四五六日].*")));
        assertTrue(messages.stream().anyMatch(message -> message.getText().contains("只能调用查询类工具")));
        assertTrue(messages.stream().anyMatch(message -> message.getText().contains("不要把“我建议下一步操作”伪装成“我已经替你执行成功”")));
    }

    @Test
    void shouldSkipSummaryWriteWhenTargetSequenceIsStale() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);

        AiChatSessionEntity freshSession = new AiChatSessionEntity();
        freshSession.setId(11L);
        freshSession.setLastSummarizedSeq(20);
        when(sessionService.getById(11L)).thenReturn(freshSession);

        service.completeSummaryUpdate(11L, 18, "旧摘要");

        verify(sessionService, never()).updateById(any());
    }

    @Test
    void shouldPersistSummaryWhenTargetSequenceAdvances() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        org.redisson.api.RedissonClient redissonClient = mock(org.redisson.api.RedissonClient.class);
        PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);
        ChatClient agentChatClient = mock(ChatClient.class);
        ChatClient analysisChatClient = mock(ChatClient.class);

        ChatAgentService service = new ChatAgentService(agentChatClient, analysisChatClient, sessionService, messageService, registrationService, redissonClient, ragOrchestrator);

        AiChatSessionEntity freshSession = new AiChatSessionEntity();
        freshSession.setId(11L);
        freshSession.setLastSummarizedSeq(10);
        when(sessionService.getById(11L)).thenReturn(freshSession);

        service.completeSummaryUpdate(11L, 18, "新摘要");

        verify(sessionService).updateById(org.mockito.ArgumentMatchers.argThat(update ->
                update.getId().equals(11L)
                        && update.getLastSummarizedSeq().equals(18)
                        && "新摘要".equals(update.getSummary())
        ));
    }
}


