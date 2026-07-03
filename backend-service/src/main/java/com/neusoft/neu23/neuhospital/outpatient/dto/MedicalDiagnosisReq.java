package com.neusoft.neu23.neuhospital.outpatient.dto;

public class MedicalDiagnosisReq {
    private String diseaseCode;
    private String diseaseName;
    private String diagnosisType;
    private Boolean suspectedFlag;

    public String getDiseaseCode() { return diseaseCode; }
    public void setDiseaseCode(String diseaseCode) { this.diseaseCode = diseaseCode; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(String diagnosisType) { this.diagnosisType = diagnosisType; }
    public Boolean getSuspectedFlag() { return suspectedFlag; }
    public void setSuspectedFlag(Boolean suspectedFlag) { this.suspectedFlag = suspectedFlag; }
}
