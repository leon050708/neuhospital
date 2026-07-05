package com.neusoft.neu23.neuhospital.outpatient.vo;

import java.time.LocalDateTime;

public class MedicalDiagnosisVO {
    private Long id;
    private String diseaseCode;
    private String diseaseName;
    private String diagnosisType;
    private Boolean suspectedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDiseaseCode() { return diseaseCode; }
    public void setDiseaseCode(String diseaseCode) { this.diseaseCode = diseaseCode; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(String diagnosisType) { this.diagnosisType = diagnosisType; }
    public Boolean getSuspectedFlag() { return suspectedFlag; }
    public void setSuspectedFlag(Boolean suspectedFlag) { this.suspectedFlag = suspectedFlag; }
}
