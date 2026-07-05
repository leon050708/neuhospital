package com.neusoft.neu23.neuhospital.payment.dto;

import java.util.List;

public class PaymentCreateReq {
    private Long patientId;
    private List<PaymentItemReq> items;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public List<PaymentItemReq> getItems() {
        return items;
    }

    public void setItems(List<PaymentItemReq> items) {
        this.items = items;
    }

    public static class PaymentItemReq {
        private String itemType; // REGISTRATION, CHECK, INSPECTION, PRESCRIPTION
        private Long bizId;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public Long getBizId() {
            return bizId;
        }

        public void setBizId(Long bizId) {
            this.bizId = bizId;
        }
    }
}
