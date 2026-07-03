package com.neusoft.neu23.neuhospital.inspection.dto;

public class DisposalRequestCreateReq {
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private Long departmentId;
    private String disposalItemCode;
    private String disposalItemName;
    private Integer quantity;
    private String remark;

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
    public String getDisposalItemCode() { return disposalItemCode; }
    public void setDisposalItemCode(String disposalItemCode) { this.disposalItemCode = disposalItemCode; }
    public String getDisposalItemName() { return disposalItemName; }
    public void setDisposalItemName(String disposalItemName) { this.disposalItemName = disposalItemName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
