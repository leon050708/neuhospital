package com.neusoft.neu23.neuhospital.inspection.vo;

import java.time.LocalDateTime;

public class CheckRequestVO {
    private Long id;
    private String requestNo;
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private Long departmentId;
    private Long targetDepartmentId;
    private String checkItemCode;
    private String checkItemName;
    private String clinicalDiagnosis;
    private String purpose;
    private Boolean urgentFlag;
    private String status;
    private String resultSummary;
    private LocalDateTime requestedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(Long medicalRecordId) { this.medicalRecordId = medicalRecordId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
