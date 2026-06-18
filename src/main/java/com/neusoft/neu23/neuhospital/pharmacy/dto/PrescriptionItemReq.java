package com.neusoft.neu23.neuhospital.pharmacy.dto;

public class PrescriptionItemReq {
    private Long drugId;
    private String dosage;
    private String frequency;
    private Integer days;
    private Integer quantity;
    private String usageMethod;

    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getUsageMethod() { return usageMethod; }
    public void setUsageMethod(String usageMethod) { this.usageMethod = usageMethod; }
}
