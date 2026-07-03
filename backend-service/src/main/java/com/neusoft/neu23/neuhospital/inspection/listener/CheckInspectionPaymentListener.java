package com.neusoft.neu23.neuhospital.inspection.listener;

import com.neusoft.neu23.neuhospital.common.event.PaymentSuccessEvent;
import com.neusoft.neu23.neuhospital.common.event.PaymentTimeoutEvent;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.CheckRequestMapper;
import com.neusoft.neu23.neuhospital.inspection.mapper.InspectionRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CheckInspectionPaymentListener {

    @Autowired
    private CheckRequestMapper checkRequestMapper;
    
    @Autowired
    private InspectionRequestMapper inspectionRequestMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getPaidItems()) {
            if ("CHECK".equals(item.getItemType())) {
                CheckRequestEntity check = checkRequestMapper.selectById(item.getSourceId());
                if (check != null && "NEW".equals(check.getStatus())) {
                    check.setStatus("PAID");
                    checkRequestMapper.updateById(check);
                }
            } else if ("INSPECTION".equals(item.getItemType())) {
                InspectionRequestEntity inspection = inspectionRequestMapper.selectById(item.getSourceId());
                if (inspection != null && "NEW".equals(inspection.getStatus())) {
                    inspection.setStatus("PAID");
                    inspectionRequestMapper.updateById(inspection);
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentTimeout(PaymentTimeoutEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getTimeoutItems()) {
            if ("CHECK".equals(item.getItemType())) {
                CheckRequestEntity check = checkRequestMapper.selectById(item.getSourceId());
                if (check != null && "NEW".equals(check.getStatus())) {
                    check.setStatus("CANCELLED");
                    checkRequestMapper.updateById(check);
                }
            } else if ("INSPECTION".equals(item.getItemType())) {
                InspectionRequestEntity inspection = inspectionRequestMapper.selectById(item.getSourceId());
                if (inspection != null && "NEW".equals(inspection.getStatus())) {
                    inspection.setStatus("CANCELLED");
                    inspectionRequestMapper.updateById(inspection);
                }
            }
        }
    }
}
