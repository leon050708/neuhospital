package com.neusoft.neu23.neuhospital.payment.controller;

import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.payment.dto.PaymentCreateReq;
import com.neusoft.neu23.neuhospital.payment.service.PaymentOrderService;
import com.neusoft.neu23.neuhospital.payment.vo.PaymentPendingItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<List<PaymentPendingItemVO>> getPendingPayments() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        return Result.success(paymentOrderService.getPendingPayments(patientId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Long> createPaymentOrder(@RequestBody PaymentCreateReq req) {
        req.setPatientId(SecurityUtils.getCurrentPatientId());
        return Result.success(paymentOrderService.createPaymentOrder(req));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> mockPaySuccess(@PathVariable Long id) {
        paymentOrderService.mockPaySuccess(id);
        return Result.success();
    }
}
