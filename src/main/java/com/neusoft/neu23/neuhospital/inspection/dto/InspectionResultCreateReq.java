package com.neusoft.neu23.neuhospital.inspection.dto;

import java.util.List;

public class InspectionResultCreateReq {
    private Long inspectionRequestId;
    private String resultSummary;
    private String conclusion;
    private List<InspectionResultItemReq> items;

    public Long getInspectionRequestId() { return inspectionRequestId; }
    public void setInspectionRequestId(Long inspectionRequestId) { this.inspectionRequestId = inspectionRequestId; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public List<InspectionResultItemReq> getItems() { return items; }
    public void setItems(List<InspectionResultItemReq> items) { this.items = items; }
}
