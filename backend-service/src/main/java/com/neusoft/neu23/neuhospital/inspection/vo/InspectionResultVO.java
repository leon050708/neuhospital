package com.neusoft.neu23.neuhospital.inspection.vo;

import java.time.LocalDateTime;
import java.util.List;

public class InspectionResultVO {
    private Long id;
    private Long inspectionRequestId;
    private String reportNo;
    private String resultSummary;
    private String conclusion;
    private Long reportDoctorId;
    private LocalDateTime reportedAt;
    private String status;
    private List<InspectionResultItemVO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInspectionRequestId() { return inspectionRequestId; }
    public void setInspectionRequestId(Long inspectionRequestId) { this.inspectionRequestId = inspectionRequestId; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public Long getReportDoctorId() { return reportDoctorId; }
    public void setReportDoctorId(Long reportDoctorId) { this.reportDoctorId = reportDoctorId; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InspectionResultItemVO> getItems() { return items; }
    public void setItems(List<InspectionResultItemVO> items) { this.items = items; }
}
