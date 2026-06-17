package com.neusoft.neu23.neuhospital.inspection.dto;

public class InspectionRequestCreateReq {
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long departmentId;
    private Long targetDepartmentId;
    private String inspectionItemCode;
    private String inspectionItemName;
    private String sampleType;
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
    public String getInspectionItemCode() { return inspectionItemCode; }
    public void setInspectionItemCode(String inspectionItemCode) { this.inspectionItemCode = inspectionItemCode; }
    public String getInspectionItemName() { return inspectionItemName; }
    public void setInspectionItemName(String inspectionItemName) { this.inspectionItemName = inspectionItemName; }
    public String getSampleType() { return sampleType; }
    public void setSampleType(String sampleType) { this.sampleType = sampleType; }
    public Boolean getUrgentFlag() { return urgentFlag; }
    public void setUrgentFlag(Boolean urgentFlag) { this.urgentFlag = urgentFlag; }
}
