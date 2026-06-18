package com.neusoft.neu23.neuhospital.pharmacy.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PrescriptionVO {
    private Long id;
    private String prescriptionNo;
    private Long patientId;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private Long departmentId;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime requestedAt;
    private List<PrescriptionItemVO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPrescriptionNo() { return prescriptionNo; }
    public void setPrescriptionNo(String prescriptionNo) { this.prescriptionNo = prescriptionNo; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public List<PrescriptionItemVO> getItems() { return items; }
    public void setItems(List<PrescriptionItemVO> items) { this.items = items; }
}
