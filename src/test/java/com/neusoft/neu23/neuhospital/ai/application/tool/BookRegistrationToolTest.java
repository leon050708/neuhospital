package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        RegistrationService registrationService = mock(RegistrationService.class);
        BookRegistrationTool tool = new BookRegistrationTool(registrationService);
        when(registrationService.quickRegister(org.mockito.ArgumentMatchers.any(RegistrationCreateReq.class)))
                .thenReturn("MSG-001");

        AiAgentSessionContext.bind(101L, 35L, 88L);

        String result = tool.bookRegistration().apply(new BookRegistrationTool.Request(66L));

        ArgumentCaptor<RegistrationCreateReq> captor = ArgumentCaptor.forClass(RegistrationCreateReq.class);
        verify(registrationService).quickRegister(captor.capture());
        assertEquals(35L, captor.getValue().getPatientId());
        assertEquals(66L, captor.getValue().getScheduleId());
        assertEquals("挂号受理成功！单号/消息ID：MSG-001", result);
    }
}
