package com.neusoft.neu23.neuhospital.ai.dto;

import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatSessionEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatSessionRespTest {

    @Test
    void shouldMapSessionEntityWithoutExposingInternalIdContract() {
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setId(101L);
        session.setSessionNo("CHAT1234567890ABCDEF1234567890ABCD");
        session.setRegistrationId(88L);
        session.setSessionType("TRIAGE");
        session.setStatus("ENABLED");

        ChatSessionResp resp = ChatSessionResp.from(session);

        assertEquals("CHAT1234567890ABCDEF1234567890ABCD", resp.getSessionNo());
        assertEquals(88L, resp.getRegistrationId());
        assertEquals("TRIAGE", resp.getSessionType());
        assertEquals("ENABLED", resp.getStatus());
    }
}
