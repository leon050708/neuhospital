package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.neusoft.neu23.neuhospital.integration.registration.RegistrationGatewayClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookRegistrationToolTest {

    @AfterEach
    void clearContext() {
        AiAgentSessionContext.clear();
    }

    @Test
    void shouldUseCurrentPatientFromSessionContextWhenBookingRegistration() {
        RegistrationGatewayClient registrationGatewayClient = mock(RegistrationGatewayClient.class);
        BookRegistrationTool tool = new BookRegistrationTool(registrationGatewayClient);
        when(registrationGatewayClient.quickRegister(35L, 66L))
                .thenReturn("MSG-001");

        AiAgentSessionContext.bind(101L, 35L, 88L);

        String result = tool.bookRegistration().apply(new BookRegistrationTool.Request(66L));

        verify(registrationGatewayClient).quickRegister(35L, 66L);
        assertEquals("挂号受理成功！单号/消息ID：MSG-001", result);
    }
}
