package com.neusoft.neu23.neuhospital.ct.vo;

import java.time.LocalDateTime;

public class CtAnalysisTaskVO {

    private Long taskId;
    private Long ctImageFileId;
    private String analysisType;
    private String taskStatus;
    private LocalDateTime submittedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getCtImageFileId() {
        return ctImageFileId;
    }

    public void setCtImageFileId(Long ctImageFileId) {
        this.ctImageFileId = ctImageFileId;
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

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
