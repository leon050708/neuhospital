package com.neusoft.neu23.neuhospital.inspection.vo;

import java.time.LocalDateTime;

public class InspectionRequestVO {
    private Long id;
    private String requestNo;
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private Long departmentId;
    private Long targetDepartmentId;
    private String inspectionItemCode;
    private String inspectionItemName;
    private String sampleType;
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
    public String getInspectionItemCode() { return inspectionItemCode; }
    public void setInspectionItemCode(String inspectionItemCode) { this.inspectionItemCode = inspectionItemCode; }
    public String getInspectionItemName() { return inspectionItemName; }
    public void setInspectionItemName(String inspectionItemName) { this.inspectionItemName = inspectionItemName; }
    public String getSampleType() { return sampleType; }
    public void setSampleType(String sampleType) { this.sampleType = sampleType; }
    public Boolean getUrgentFlag() { return urgentFlag; }
    public void setUrgentFlag(Boolean urgentFlag) { this.urgentFlag = urgentFlag; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
