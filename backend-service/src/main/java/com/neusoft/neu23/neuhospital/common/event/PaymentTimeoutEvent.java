package com.neusoft.neu23.neuhospital.common.event;

import org.springframework.context.ApplicationEvent;
import java.util.List;

public class PaymentTimeoutEvent extends ApplicationEvent {

    private final String orderNo;
    private final List<PaymentSuccessEvent.PaymentItemInfo> timeoutItems;

    public PaymentTimeoutEvent(Object source, String orderNo, List<PaymentSuccessEvent.PaymentItemInfo> timeoutItems) {
        super(source);
        this.orderNo = orderNo;
        this.timeoutItems = timeoutItems;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public List<PaymentSuccessEvent.PaymentItemInfo> getTimeoutItems() {
        return timeoutItems;
    }
}
