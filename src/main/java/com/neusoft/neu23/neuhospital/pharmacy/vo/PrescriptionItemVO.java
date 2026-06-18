package com.neusoft.neu23.neuhospital.pharmacy.vo;

import java.math.BigDecimal;

public class PrescriptionItemVO {
    private Long id;
    private Long prescriptionId;
    private Long drugId;
    private String drugName;
    private String specification;
    private String dosage;
    private String frequency;
    private Integer days;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String usageMethod;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getUsageMethod() { return usageMethod; }
    public void setUsageMethod(String usageMethod) { this.usageMethod = usageMethod; }
}
