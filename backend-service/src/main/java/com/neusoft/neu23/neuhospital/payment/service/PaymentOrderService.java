package com.neusoft.neu23.neuhospital.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.neu23.neuhospital.payment.dto.PaymentCreateReq;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentOrderEntity;
import com.neusoft.neu23.neuhospital.payment.vo.PaymentPendingItemVO;

import java.util.List;

public interface PaymentOrderService extends IService<PaymentOrderEntity> {
    List<PaymentPendingItemVO> getPendingPayments(Long patientId);
    Long createPaymentOrder(PaymentCreateReq req);
    void mockPaySuccess(Long orderId);
    void checkTimeoutOrders(); // scheduled task method
}
