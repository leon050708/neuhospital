package com.neusoft.neu23.neuhospital.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class PaymentSuccessEvent extends ApplicationEvent {

    private final String orderNo;
    private final List<PaymentItemInfo> paidItems;

    public PaymentSuccessEvent(Object source, String orderNo, List<PaymentItemInfo> paidItems) {
        super(source);
        this.orderNo = orderNo;
        this.paidItems = paidItems;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public List<PaymentItemInfo> getPaidItems() {
        return paidItems;
    }

    public static class PaymentItemInfo {
        private String itemType; // e.g., "PRESCRIPTION", "CHECK_REQUEST", "INSPECTION_REQUEST"
        private Long sourceId;

        public PaymentItemInfo(String itemType, Long sourceId) {
            this.itemType = itemType;
            this.sourceId = sourceId;
        }

        public String getItemType() { return itemType; }
        public Long getSourceId() { return sourceId; }
    }
}
