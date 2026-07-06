package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.domain.entity.AiChatMessageEntity;

import java.util.List;

public record PatientAgentContextWindow(
        String currentMessage,
        String sessionSummary,
        List<AiChatMessageEntity> recentMessages
) {

    public String renderForPrompt() {
        StringBuilder builder = new StringBuilder();
        if (sessionSummary != null && !sessionSummary.isBlank()) {
            builder.append("历史摘要:\n").append(sessionSummary.trim()).append("\n\n");
        }
        if (recentMessages != null && !recentMessages.isEmpty()) {
            builder.append("最近对话:\n");
            for (AiChatMessageEntity message : recentMessages) {
                builder.append(message.getMessageRole())
                        .append(": ")
                        .append(message.getMessageContent())
                        .append('\n');
            }
            builder.append('\n');
        }
        builder.append("当前问题:\n").append(currentMessage == null ? "" : currentMessage.trim());
        return builder.toString();
    }
}
