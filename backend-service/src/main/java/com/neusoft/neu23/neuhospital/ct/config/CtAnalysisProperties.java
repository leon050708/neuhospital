package com.neusoft.neu23.neuhospital.ct.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ct-analysis")
public class CtAnalysisProperties {

    private String pythonExecutable = "/opt/anaconda3/envs/neuedu/bin/python";
    private String scriptPath = ".md/ct/infer_b1_b2.py";
    private String modelPath = ".md/ct/best_b1_b2_model.pth";
    private Integer timeoutSeconds = 300;
    private String localCacheDir = "datas/ct-analysis-cache";

    public String getPythonExecutable() {
        return pythonExecutable;
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getLocalCacheDir() {
        return localCacheDir;
    }

    public void setLocalCacheDir(String localCacheDir) {
        this.localCacheDir = localCacheDir;
    }
}
