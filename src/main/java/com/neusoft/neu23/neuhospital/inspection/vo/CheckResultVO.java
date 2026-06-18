package com.neusoft.neu23.neuhospital.inspection.vo;

import java.time.LocalDateTime;

public class CheckResultVO {
    private Long id;
    private Long checkRequestId;
    private String reportNo;
    private String resultText;
    private String resultSummary;
    private String conclusion;
    private Long reportFileId;
    private Long reportDoctorId;
    private LocalDateTime reportedAt;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCheckRequestId() { return checkRequestId; }
    public void setCheckRequestId(Long checkRequestId) { this.checkRequestId = checkRequestId; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public Long getReportFileId() { return reportFileId; }
    public void setReportFileId(Long reportFileId) { this.reportFileId = reportFileId; }
    public Long getReportDoctorId() { return reportDoctorId; }
    public void setReportDoctorId(Long reportDoctorId) { this.reportDoctorId = reportDoctorId; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
