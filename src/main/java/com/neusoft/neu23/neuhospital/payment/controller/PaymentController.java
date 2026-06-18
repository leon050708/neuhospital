package com.neusoft.neu23.neuhospital.payment.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.payment.dto.PaymentCreateReq;
import com.neusoft.neu23.neuhospital.payment.service.PaymentOrderService;
import com.neusoft.neu23.neuhospital.payment.vo.PaymentPendingItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @GetMapping("/pending")
    public Result<List<PaymentPendingItemVO>> getPendingPayments(@RequestParam Long patientId) {
        return Result.success(paymentOrderService.getPendingPayments(patientId));
    }

    @PostMapping("/create")
    public Result<Long> createPaymentOrder(@RequestBody PaymentCreateReq req) {
        return Result.success(paymentOrderService.createPaymentOrder(req));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> mockPaySuccess(@PathVariable Long id) {
        paymentOrderService.mockPaySuccess(id);
        return Result.success();
    }
}
