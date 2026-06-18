package com.neusoft.neu23.neuhospital.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neu23.neuhospital.common.event.PaymentSuccessEvent;
import com.neusoft.neu23.neuhospital.common.event.PaymentTimeoutEvent;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.inspection.entity.CheckRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.entity.InspectionRequestEntity;
import com.neusoft.neu23.neuhospital.inspection.mapper.CheckRequestMapper;
import com.neusoft.neu23.neuhospital.inspection.mapper.InspectionRequestMapper;
import com.neusoft.neu23.neuhospital.payment.dto.PaymentCreateReq;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentItemEntity;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentOrderEntity;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentItemMapper;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentOrderMapper;
import com.neusoft.neu23.neuhospital.payment.service.PaymentOrderService;
import com.neusoft.neu23.neuhospital.payment.vo.PaymentPendingItemVO;
import com.neusoft.neu23.neuhospital.pharmacy.entity.PrescriptionEntity;
import com.neusoft.neu23.neuhospital.pharmacy.mapper.PrescriptionMapper;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@EnableScheduling
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrderEntity> implements PaymentOrderService {

    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private CheckRequestMapper checkRequestMapper;
    @Autowired
    private InspectionRequestMapper inspectionRequestMapper;
    @Autowired
    private PrescriptionMapper prescriptionMapper;
    @Autowired
    private PaymentItemMapper paymentItemMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public List<PaymentPendingItemVO> getPendingPayments(Long patientId) {
        List<PaymentPendingItemVO> pendingList = new ArrayList<>();

        // 1. 扫描未付的挂号费
        List<RegistrationEntity> regs = registrationMapper.selectList(
                new LambdaQueryWrapper<RegistrationEntity>()
                        .eq(RegistrationEntity::getPatientId, patientId)
                        .eq(RegistrationEntity::getStatus, "UNPAID")
        );
        for (RegistrationEntity r : regs) {
            PaymentPendingItemVO vo = new PaymentPendingItemVO();
            vo.setItemType("REGISTRATION");
            vo.setBizId(r.getId());
            vo.setItemName("挂号费-" + r.getRegistrationNo());
            vo.setAmount(r.getFeeAmount());
            vo.setCreatedAt(r.getCreatedAt());
            pendingList.add(vo);
        }

        // 2. 扫描未付的检查费
        List<CheckRequestEntity> checks = checkRequestMapper.selectList(
                new LambdaQueryWrapper<CheckRequestEntity>()
                        .eq(CheckRequestEntity::getPatientId, patientId)
                        .eq(CheckRequestEntity::getStatus, "NEW")
        );
        for (CheckRequestEntity c : checks) {
            PaymentPendingItemVO vo = new PaymentPendingItemVO();
            vo.setItemType("CHECK");
            vo.setBizId(c.getId());
            vo.setItemName("检查费-" + c.getCheckItemName());
            vo.setAmount(c.getFeeAmount() != null ? c.getFeeAmount() : new BigDecimal("100.00")); // 默认100元
            vo.setCreatedAt(c.getCreatedAt());
            pendingList.add(vo);
        }

        // 3. 扫描未付的检验费
        List<InspectionRequestEntity> inspections = inspectionRequestMapper.selectList(
                new LambdaQueryWrapper<InspectionRequestEntity>()
                        .eq(InspectionRequestEntity::getPatientId, patientId)
                        .eq(InspectionRequestEntity::getStatus, "NEW")
        );
        for (InspectionRequestEntity i : inspections) {
            PaymentPendingItemVO vo = new PaymentPendingItemVO();
            vo.setItemType("INSPECTION");
            vo.setBizId(i.getId());
            vo.setItemName("检验费-" + i.getInspectionItemName());
            vo.setAmount(i.getFeeAmount() != null ? i.getFeeAmount() : new BigDecimal("80.00")); // 默认80元
            vo.setCreatedAt(i.getCreatedAt());
            pendingList.add(vo);
        }

        // 4. 扫描未付的处方费
        List<PrescriptionEntity> prescriptions = prescriptionMapper.selectList(
                new LambdaQueryWrapper<PrescriptionEntity>()
                        .eq(PrescriptionEntity::getPatientId, patientId)
                        .eq(PrescriptionEntity::getStatus, "NEW")
        );
        for (PrescriptionEntity p : prescriptions) {
            PaymentPendingItemVO vo = new PaymentPendingItemVO();
            vo.setItemType("PRESCRIPTION");
            vo.setBizId(p.getId());
            vo.setItemName("药费-" + p.getPrescriptionNo());
            vo.setAmount(p.getTotalAmount());
            vo.setCreatedAt(p.getCreatedAt());
            pendingList.add(vo);
        }

        return pendingList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaymentOrder(PaymentCreateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException(400, "缴费明细不能为空");
        }
        
        List<PaymentPendingItemVO> allPendings = getPendingPayments(req.getPatientId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setOrderNo("PAY" + System.currentTimeMillis());
        order.setPatientId(req.getPatientId());
        order.setPayStatus("UNPAID");
        order.setCreatedAt(LocalDateTime.now());
        this.save(order);

        for (PaymentCreateReq.PaymentItemReq itemReq : req.getItems()) {
            // 校验并在所有未付款项中匹配
            PaymentPendingItemVO matchedVO = allPendings.stream()
                    .filter(p -> p.getItemType().equals(itemReq.getItemType()) && p.getBizId().equals(itemReq.getBizId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(400, "包含非法或已缴费的项目: " + itemReq.getItemType() + " ID:" + itemReq.getBizId()));
            
            PaymentItemEntity item = new PaymentItemEntity();
            item.setPaymentOrderId(order.getId());
            item.setItemType(matchedVO.getItemType());
            item.setBizId(matchedVO.getBizId());
            item.setItemName(matchedVO.getItemName());
            item.setAmount(matchedVO.getAmount());
            item.setQuantity(1);
            item.setUnitPrice(matchedVO.getAmount());
            item.setStatus("UNPAID");
            paymentItemMapper.insert(item);
            
            totalAmount = totalAmount.add(matchedVO.getAmount());
        }

        order.setTotalAmount(totalAmount);
        this.updateById(order);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(Long orderId) {
        PaymentOrderEntity order = this.getById(orderId);
        if (order == null || !"UNPAID".equals(order.getPayStatus())) {
            throw new BusinessException(400, "支付单不存在或不是待支付状态");
        }

        order.setPayStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setPayChannel("MOCK_WECHAT");
        this.updateById(order);

        List<PaymentItemEntity> items = paymentItemMapper.selectList(
                new LambdaQueryWrapper<PaymentItemEntity>().eq(PaymentItemEntity::getPaymentOrderId, orderId)
        );
        List<PaymentSuccessEvent.PaymentItemInfo> paidItemsList = new ArrayList<>();
        for (PaymentItemEntity item : items) {
            item.setStatus("PAID");
            paymentItemMapper.updateById(item);
            paidItemsList.add(new PaymentSuccessEvent.PaymentItemInfo(item.getItemType(), item.getBizId()));
        }
        
        // 发出支付成功广播，包含所有已支付的明细
        PaymentSuccessEvent event = new PaymentSuccessEvent(this, order.getOrderNo(), paidItemsList);
        eventPublisher.publishEvent(event);
    }

    @Override
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    @Transactional(rollbackFor = Exception.class)
    public void checkTimeoutOrders() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);
        List<PaymentOrderEntity> timeoutOrders = this.list(
                new LambdaQueryWrapper<PaymentOrderEntity>()
                        .eq(PaymentOrderEntity::getPayStatus, "UNPAID")
                        .le(PaymentOrderEntity::getCreatedAt, timeoutThreshold)
        );

        for (PaymentOrderEntity order : timeoutOrders) {
            order.setPayStatus("CANCELLED");
            this.updateById(order);

            List<PaymentItemEntity> items = paymentItemMapper.selectList(
                    new LambdaQueryWrapper<PaymentItemEntity>().eq(PaymentItemEntity::getPaymentOrderId, order.getId())
            );
            List<PaymentSuccessEvent.PaymentItemInfo> timeoutItemsList = new ArrayList<>();
            for (PaymentItemEntity item : items) {
                item.setStatus("CANCELLED");
                paymentItemMapper.updateById(item);
                timeoutItemsList.add(new PaymentSuccessEvent.PaymentItemInfo(item.getItemType(), item.getBizId()));
            }

            // 发送超时取消广播，业务模块（如挂号）监听到后回滚库存
            PaymentTimeoutEvent event = new PaymentTimeoutEvent(this, order.getOrderNo(), timeoutItemsList);
            eventPublisher.publishEvent(event);
        }
    }
}
