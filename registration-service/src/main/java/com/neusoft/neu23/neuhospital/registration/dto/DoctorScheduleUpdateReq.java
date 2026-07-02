package com.neusoft.neu23.neuhospital.registration.dto;

import java.math.BigDecimal;

public class DoctorScheduleUpdateReq {
    private Integer sourceCount;
    private BigDecimal feeAmount;
    private String sourceType;
    private String status; // ENABLED, DISABLED, CLOSED

    public Integer getSourceCount() { return sourceCount; }
    public void setSourceCount(Integer sourceCount) { this.sourceCount = sourceCount; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
