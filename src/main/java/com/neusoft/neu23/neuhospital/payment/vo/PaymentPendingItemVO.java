package com.neusoft.neu23.neuhospital.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentPendingItemVO {
    private String itemType; // REGISTRATION, CHECK, INSPECTION, PRESCRIPTION
    private Long bizId;
    private String itemName;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
