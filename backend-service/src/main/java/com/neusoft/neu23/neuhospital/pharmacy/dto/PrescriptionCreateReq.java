package com.neusoft.neu23.neuhospital.pharmacy.dto;

import java.util.List;

public class PrescriptionCreateReq {
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private Long departmentId;
    private String remark;
    private List<PrescriptionItemReq> items;

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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<PrescriptionItemReq> getItems() { return items; }
    public void setItems(List<PrescriptionItemReq> items) { this.items = items; }
}
