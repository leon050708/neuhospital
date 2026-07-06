package com.neusoft.neu23.neuhospital.ai.application.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.models")
public class AiModelProperties {

    private String agentModel;
    private String analysisModel;

    public String getAgentModel() {
        return agentModel;
    }

    public void setAgentModel(String agentModel) {
        this.agentModel = agentModel;
    }

    public String getAnalysisModel() {
        return analysisModel;
    }

    public void setAnalysisModel(String analysisModel) {
        this.analysisModel = analysisModel;
    }
}
