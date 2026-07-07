package com.neusoft.neu23.neuhospital.ai.application.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatClientConfig {

    @Bean("agentChatClient")
    public ChatClient agentChatClient(ChatClient.Builder rootBuilder, AiModelProperties properties) {
        return rootBuilder.clone()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAgentModel())
                        .build())
                .defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule")
                .build();
    }

    @Bean("analysisChatClient")
    public ChatClient analysisChatClient(ChatClient.Builder rootBuilder, AiModelProperties properties) {
        return rootBuilder.clone()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAnalysisModel())
                        .build())
                .build();
    }
}
