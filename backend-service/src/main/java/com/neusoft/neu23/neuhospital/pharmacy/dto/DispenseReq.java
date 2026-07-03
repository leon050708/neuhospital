package com.neusoft.neu23.neuhospital.pharmacy.dto;

import java.util.List;

public class DispenseReq {
    private Long prescriptionId;
    private Long pharmacyUserId;

    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Long getPharmacyUserId() { return pharmacyUserId; }
    public void setPharmacyUserId(Long pharmacyUserId) { this.pharmacyUserId = pharmacyUserId; }
}
