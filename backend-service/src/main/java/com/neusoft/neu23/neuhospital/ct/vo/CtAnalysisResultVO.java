package com.neusoft.neu23.neuhospital.ct.vo;

import java.math.BigDecimal;
import java.util.Map;

public class CtAnalysisResultVO {

    private Long taskId;
    private String analysisType;
    private String taskStatus;
    private String predictedCategory;
    private BigDecimal confidence;
    private Map<String, BigDecimal> classProbabilities;
    private String riskLevel;
    private String modelName;
    private String doctorConfirmStatus;
    private String failureReason;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Map<String, BigDecimal> getClassProbabilities() {
        return classProbabilities;
    }

    public void setClassProbabilities(Map<String, BigDecimal> classProbabilities) {
        this.classProbabilities = classProbabilities;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDoctorConfirmStatus() {
        return doctorConfirmStatus;
    }

    public void setDoctorConfirmStatus(String doctorConfirmStatus) {
        this.doctorConfirmStatus = doctorConfirmStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
