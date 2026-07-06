package com.neusoft.neu23.neuhospital.ai.application.agent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatClientConfigTest {

    @Test
    void shouldBuildDedicatedAgentAndAnalysisClientsWithDifferentModels() {
        ChatClient.Builder rootBuilder = mock(ChatClient.Builder.class);
        ChatClient.Builder agentBuilder = mock(ChatClient.Builder.class);
        ChatClient.Builder analysisBuilder = mock(ChatClient.Builder.class);
        ChatClient agentClient = mock(ChatClient.class);
        ChatClient analysisClient = mock(ChatClient.class);

        when(rootBuilder.clone()).thenReturn(agentBuilder, analysisBuilder);
        when(agentBuilder.defaultOptions(any())).thenReturn(agentBuilder);
        when(agentBuilder.defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule"))
                .thenReturn(agentBuilder);
        when(agentBuilder.build()).thenReturn(agentClient);
        when(analysisBuilder.defaultOptions(any())).thenReturn(analysisBuilder);
        when(analysisBuilder.build()).thenReturn(analysisClient);

        AiModelProperties properties = new AiModelProperties();
        properties.setAgentModel("qwen-plus");
        properties.setAnalysisModel("qwen-turbo");

        AiChatClientConfig config = new AiChatClientConfig();

        ChatClient builtAgentClient = config.agentChatClient(rootBuilder, properties);
        ChatClient builtAnalysisClient = config.analysisChatClient(rootBuilder, properties);

        assertSame(agentClient, builtAgentClient);
        assertSame(analysisClient, builtAnalysisClient);

        ArgumentCaptor<OpenAiChatOptions> agentOptionsCaptor = ArgumentCaptor.forClass(OpenAiChatOptions.class);
        verify(agentBuilder).defaultOptions(agentOptionsCaptor.capture());
        assertEquals("qwen-plus", agentOptionsCaptor.getValue().getModel());

        ArgumentCaptor<OpenAiChatOptions> analysisOptionsCaptor = ArgumentCaptor.forClass(OpenAiChatOptions.class);
        verify(analysisBuilder).defaultOptions(analysisOptionsCaptor.capture());
        assertEquals("qwen-turbo", analysisOptionsCaptor.getValue().getModel());

        verify(agentBuilder).defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule");
        verify(analysisBuilder, never()).defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule");
        verify(analysisBuilder, never()).defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration");
    }
}
