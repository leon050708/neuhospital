package com.neusoft.neu23.neuhospital.ct.dto;

public class CtAnalysisTaskCreateReq {

    private Long ctImageFileId;
    private String analysisType;

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
}
