package com.neusoft.neu23.neuhospital.outpatient.dto;

import java.util.List;

public class MedicalRecordUpdateReq {
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String allergyHistory;
    private String physicalExam;
    private String preliminaryDiagnosis;
    private String finalDiagnosis;
    private String advice;
    
    private List<MedicalDiagnosisReq> diagnoses;

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
    public String getFinalDiagnosis() { return finalDiagnosis; }
    public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public List<MedicalDiagnosisReq> getDiagnoses() { return diagnoses; }
    public void setDiagnoses(List<MedicalDiagnosisReq> diagnoses) { this.diagnoses = diagnoses; }
}
