package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAgentServiceTest {

    @Test
    void shouldRejectChatWhenSessionDoesNotBelongToCurrentPatient() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);

        when(builder.defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration"))
                .thenReturn(builder);
        when(builder.build()).thenReturn(client);

        ChatAgentService service = new ChatAgentService(builder, sessionService, messageService, registrationService);

        assertThrows(IllegalArgumentException.class, () -> service.chat("CHAT1234567890ABCDEF1234567890ABCD", 35L, "我头疼"));
    }

    @Test
    void shouldRejectSessionCreationWhenRegistrationBelongsToAnotherPatient() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);

        when(builder.defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration"))
                .thenReturn(builder);
        when(builder.build()).thenReturn(client);

        RegistrationEntity registration = new RegistrationEntity();
        registration.setId(88L);
        registration.setPatientId(99L);
        when(registrationService.getRegistrationById(88L)).thenReturn(registration);

        ChatAgentService service = new ChatAgentService(builder, sessionService, messageService, registrationService);

        assertThrows(IllegalArgumentException.class, () -> service.createSession(35L, 88L, "TRIAGE"));
    }

    @Test
    void shouldGenerateNonEnumerableSessionNoWhenCreatingSession() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);

        when(builder.defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration"))
                .thenReturn(builder);
        when(builder.build()).thenReturn(client);

        ChatAgentService service = new ChatAgentService(builder, sessionService, messageService, registrationService);

        service.createSession(35L, null, "TRIAGE");

        ArgumentCaptor<AiChatSessionEntity> captor = ArgumentCaptor.forClass(AiChatSessionEntity.class);
        verify(sessionService).save(captor.capture());
        String sessionNo = captor.getValue().getSessionNo();
        assertNotNull(sessionNo);
        assertTrue(sessionNo.matches("CHAT[a-f0-9]{32}"));
    }
}
