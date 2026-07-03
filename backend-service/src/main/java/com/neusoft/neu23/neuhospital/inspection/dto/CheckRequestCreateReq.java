package com.neusoft.neu23.neuhospital.inspection.dto;

public class CheckRequestCreateReq {
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long departmentId;
    private Long targetDepartmentId;
    private String checkItemCode;
    private String checkItemName;
    private String clinicalDiagnosis;
    private String purpose;
    private Boolean urgentFlag;

    // getters and setters
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(Long medicalRecordId) { this.medicalRecordId = medicalRecordId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Long targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }
    public String getCheckItemCode() { return checkItemCode; }
    public void setCheckItemCode(String checkItemCode) { this.checkItemCode = checkItemCode; }
    public String getCheckItemName() { return checkItemName; }
    public void setCheckItemName(String checkItemName) { this.checkItemName = checkItemName; }
    public String getClinicalDiagnosis() { return clinicalDiagnosis; }
    public void setClinicalDiagnosis(String clinicalDiagnosis) { this.clinicalDiagnosis = clinicalDiagnosis; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Boolean getUrgentFlag() { return urgentFlag; }
    public void setUrgentFlag(Boolean urgentFlag) { this.urgentFlag = urgentFlag; }
}
