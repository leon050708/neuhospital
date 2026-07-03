package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.integration.patient.PatientGatewayClient;
import com.neusoft.neu23.neuhospital.integration.patient.RemotePatientProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientInfoToolTest {

    @AfterEach
    void clearContext() {
        AiAgentSessionContext.clear();
    }

    @Test
    void shouldReadPatientInfoFromSessionContextInsteadOfModelInput() {
        PatientGatewayClient patientGatewayClient = mock(PatientGatewayClient.class);
        PatientInfoTool tool = new PatientInfoTool(patientGatewayClient);

        RemotePatientProfile patient = new RemotePatientProfile();
        patient.setName("张三");
        when(patientGatewayClient.getPatientById(35L)).thenReturn(patient);

        AiAgentSessionContext.bind(101L, 35L, 88L);

        String result = tool.getPatientInfo().apply(new PatientInfoTool.Request());

        verify(patientGatewayClient).getPatientById(35L);
        assertTrue(result.contains("张三"));
    }
}
