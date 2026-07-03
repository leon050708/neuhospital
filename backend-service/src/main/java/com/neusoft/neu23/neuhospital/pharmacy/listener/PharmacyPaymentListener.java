package com.neusoft.neu23.neuhospital.pharmacy.listener;

import com.neusoft.neu23.neuhospital.common.event.PaymentSuccessEvent;
import com.neusoft.neu23.neuhospital.common.event.PaymentTimeoutEvent;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PharmacyPaymentListener {

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getPaidItems()) {
            if ("PRESCRIPTION".equals(item.getItemType())) {
                PrescriptionEntity prescription = prescriptionMapper.selectById(item.getSourceId());
                if (prescription != null && "NEW".equals(prescription.getStatus())) {
                    prescription.setStatus("PAID");
                    prescriptionMapper.updateById(prescription);
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentTimeout(PaymentTimeoutEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getTimeoutItems()) {
            if ("PRESCRIPTION".equals(item.getItemType())) {
                PrescriptionEntity prescription = prescriptionMapper.selectById(item.getSourceId());
                if (prescription != null && "NEW".equals(prescription.getStatus())) {
                    prescription.setStatus("CANCELLED");
                    prescriptionMapper.updateById(prescription);
                }
            }
        }
    }
}
