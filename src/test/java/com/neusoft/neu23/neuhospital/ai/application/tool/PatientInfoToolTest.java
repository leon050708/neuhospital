package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.ai.application.agent.AiAgentSessionContext;
import com.neusoft.neu23.neuhospital.patient.service.PatientService;
import com.neusoft.neu23.neuhospital.patient.vo.PatientVO;
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
        PatientService patientService = mock(PatientService.class);
        PatientInfoTool tool = new PatientInfoTool(patientService);

        PatientVO patient = new PatientVO();
        patient.setName("张三");
        when(patientService.getPatientById(35L)).thenReturn(patient);

        AiAgentSessionContext.bind(101L, 35L, 88L);

        String result = tool.getPatientInfo().apply(new PatientInfoTool.Request());

        verify(patientService).getPatientById(35L);
        assertTrue(result.contains("张三"));
    }
}
