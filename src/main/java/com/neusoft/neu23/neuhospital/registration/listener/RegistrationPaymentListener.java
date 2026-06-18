package com.neusoft.neu23.neuhospital.registration.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.common.event.PaymentSuccessEvent;
import com.neusoft.neu23.neuhospital.common.event.PaymentTimeoutEvent;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.entity.VisitQueueEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;

@Component
public class RegistrationPaymentListener {

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private RegistrationService registrationService;
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getPaidItems()) {
            if ("REGISTRATION".equals(item.getItemType())) {
                RegistrationEntity reg = registrationService.getRegistrationById(item.getSourceId());
                if (reg != null && "UNPAID".equals(reg.getStatus())) {
                    reg.setStatus("PAID");
                    registrationMapper.updateById(reg);

                    // 如果是当日号，自动签到入队列
                    if (reg.getVisitDate() != null && reg.getVisitDate().equals(LocalDate.now())) {
                        registrationService.checkIn(reg.getId(), null);
                    }
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPaymentTimeout(PaymentTimeoutEvent event) {
        for (PaymentSuccessEvent.PaymentItemInfo item : event.getTimeoutItems()) {
            if ("REGISTRATION".equals(item.getItemType())) {
                RegistrationEntity reg = registrationMapper.selectById(item.getSourceId());
                if (reg != null && "UNPAID".equals(reg.getStatus())) {
                    reg.setStatus("CANCELLED");
                    registrationMapper.updateById(reg);

                    // 回滚 Redis 库存补偿
                    String stockKey = "schedule:stock:" + reg.getScheduleId();
                    redisTemplate.opsForValue().increment(stockKey);
                }
            }
        }
    }
}
