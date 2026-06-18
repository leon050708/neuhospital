package com.neusoft.neu23.neuhospital.inspection.vo;

public class InspectionResultItemVO {
    private Long id;
    private Long inspectionResultId;
    private String itemCode;
    private String itemName;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private String abnormalFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInspectionResultId() { return inspectionResultId; }
    public void setInspectionResultId(Long inspectionResultId) { this.inspectionResultId = inspectionResultId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public String getAbnormalFlag() { return abnormalFlag; }
    public void setAbnormalFlag(String abnormalFlag) { this.abnormalFlag = abnormalFlag; }
}
