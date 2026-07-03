package com.neusoft.neu23.neuhospital.outpatient.dto;

import java.util.List;

public class MedicalRecordCreateReq {
    private Long registrationId;
    private Long patientId;
    private Long doctorId;
    private Long departmentId;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String allergyHistory;
    private String physicalExam;
    private String preliminaryDiagnosis;
    
    // 如果一次性传入明细
    private List<MedicalDiagnosisReq> diagnoses;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public String getPresentIllness() { return presentIllness; }
    public void setPresentIllness(String presentIllness) { this.presentIllness = presentIllness; }
    public String getPastHistory() { return pastHistory; }
    public void setPastHistory(String pastHistory) { this.pastHistory = pastHistory; }
    public String getAllergyHistory() { return allergyHistory; }
    public void setAllergyHistory(String allergyHistory) { this.allergyHistory = allergyHistory; }
    public String getPhysicalExam() { return physicalExam; }
    public void setPhysicalExam(String physicalExam) { this.physicalExam = physicalExam; }
    public String getPreliminaryDiagnosis() { return preliminaryDiagnosis; }
    public void setPreliminaryDiagnosis(String preliminaryDiagnosis) { this.preliminaryDiagnosis = preliminaryDiagnosis; }
    public List<MedicalDiagnosisReq> getDiagnoses() { return diagnoses; }
    public void setDiagnoses(List<MedicalDiagnosisReq> diagnoses) { this.diagnoses = diagnoses; }
}
