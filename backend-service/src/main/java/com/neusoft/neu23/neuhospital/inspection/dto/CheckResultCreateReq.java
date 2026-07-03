package com.neusoft.neu23.neuhospital.inspection.dto;

public class CheckResultCreateReq {
    private Long checkRequestId;
    private String resultText;
    private String resultSummary;
    private String conclusion;
    private Long reportFileId;

    public Long getCheckRequestId() { return checkRequestId; }
    public void setCheckRequestId(Long checkRequestId) { this.checkRequestId = checkRequestId; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public Long getReportFileId() { return reportFileId; }
    public void setReportFileId(Long reportFileId) { this.reportFileId = reportFileId; }
}
