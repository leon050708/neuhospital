package com.neusoft.neu23.neuhospital.ct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ct_analysis_result")
public class CtAnalysisResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String analysisType;
    private String predictedCategory;
    private BigDecimal confidence;
    private BigDecimal probabilityB1;
    private BigDecimal probabilityB2;
    private String riskLevel;
    private String modelName;
    private String doctorConfirmStatus;
    private String rawResultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getProbabilityB1() {
        return probabilityB1;
    }

    public void setProbabilityB1(BigDecimal probabilityB1) {
        this.probabilityB1 = probabilityB1;
    }

    public BigDecimal getProbabilityB2() {
        return probabilityB2;
    }

    public void setProbabilityB2(BigDecimal probabilityB2) {
        this.probabilityB2 = probabilityB2;
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

    public String getRawResultJson() {
        return rawResultJson;
    }

    public void setRawResultJson(String rawResultJson) {
        this.rawResultJson = rawResultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
