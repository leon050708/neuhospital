package com.neusoft.neu23.neuhospital.payment.dto;

import java.util.List;

public class PaymentOrderCreateReq {
    private Long patientId;
    private Long registrationId;
    private List<PaymentItemReq> items;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public List<PaymentItemReq> getItems() { return items; }
    public void setItems(List<PaymentItemReq> items) { this.items = items; }

    public static class PaymentItemReq {
        private String itemType; // CHECK_REQUEST, INSPECTION_REQUEST, PRESCRIPTION
        private Long bizId;

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public Long getBizId() { return bizId; }
        public void setBizId(Long bizId) { this.bizId = bizId; }
    }
}
