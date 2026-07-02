package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.doctor.entity.DoctorEntity;
import com.neusoft.neu23.neuhospital.doctor.mapper.DoctorMapper;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentItemEntity;
import com.neusoft.neu23.neuhospital.payment.entity.PaymentOrderEntity;
import com.neusoft.neu23.neuhospital.payment.entity.RefundRecordEntity;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentItemMapper;
import com.neusoft.neu23.neuhospital.payment.mapper.PaymentOrderMapper;
import com.neusoft.neu23.neuhospital.payment.mapper.RefundRecordMapper;
import com.neusoft.neu23.neuhospital.patient.entity.PatientEntity;
import com.neusoft.neu23.neuhospital.patient.mapper.PatientMapper;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.entity.VisitQueueEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.DoctorScheduleMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import com.neusoft.neu23.neuhospital.registration.mapper.VisitQueueMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RegistrationMessageLogMapper messageLogMapper;
    
    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;
    
    @Autowired
    private RegistrationMapper registrationMapper;
    
    @Autowired
    private VisitQueueMapper visitQueueMapper;

    @Autowired
    private PaymentItemMapper paymentItemMapper;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private PatientMapper patientMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    private DefaultRedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setResultType(Long.class);
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String quickRegister(RegistrationCreateReq req) {
        Long scheduleId = req.getScheduleId();
        Long patientId = req.getPatientId();

        // 1. 令牌桶限流检查
        String limitKey = "rate_limit:registration:" + scheduleId;
        Long result = redisTemplate.execute(rateLimitScript, Collections.singletonList(limitKey), "200");
        if (result != null && result == 0) {
            throw new RuntimeException("当前抢号人数过多，请稍后再试");
        }

        // 2. 防重防刷幂等性检查 (分布式锁)
        String lockKey = "req:lock:" + patientId + ":" + scheduleId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试加锁，最多等待0秒，上锁以后5秒自动解锁 (防止同一患者疯狂点击)
            if (!lock.tryLock(0, 5, TimeUnit.SECONDS)) {
                throw new RuntimeException("您操作太快了，请稍后重试");
            }

            ensureNoDuplicateRegistration(scheduleId, patientId);

            // 3. Redis 库存预扣减
            String stockKey = "schedule:stock:" + scheduleId;
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock != null && stock < 0) {
                // 扣成负数，把库存加回来保证Redis数据整洁
                redisTemplate.opsForValue().increment(stockKey);
                throw new RuntimeException("手慢了，当前排班号源已满");
            }
            registerRollbackCompensation(stockKey, scheduleId, patientId);

            // 4. 成功抢到 Redis 号源，写入本地消息表 (兜底，保证最终一致性)
            String msgId = UUID.randomUUID().toString().replace("-", "");
            RegistrationMessageLogEntity log = new RegistrationMessageLogEntity();
            log.setMsgId(msgId);
            log.setScheduleId(scheduleId);
            log.setPatientId(patientId);
            log.setPayload("{\"scheduleId\":"+scheduleId+", \"patientId\":"+patientId+"}");
            log.setStatus(0); // 待发送
            log.setRetryCount(0);
            log.setCreateTime(LocalDateTime.now());
            log.setUpdateTime(LocalDateTime.now());
            int inserted = messageLogMapper.insertWithJsonPayload(log);
            if (inserted != 1) {
                throw new RuntimeException("挂号受理失败，请稍后重试");
            }

            return msgId; // 返回挂号受理号（消息ID）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统异常，请重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void registerRollbackCompensation(String stockKey, Long scheduleId, Long patientId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try {
                        redisTemplate.opsForValue().increment(stockKey);
                    } catch (Exception ex) {
                        log.error("挂号事务回滚后回补 Redis 库存失败, scheduleId={}, patientId={}", scheduleId, patientId, ex);
                    }
                }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRegistrationMessage(String msgId, Long scheduleId, Long patientId) {
        RegistrationMessageLogEntity messageLog = messageLogMapper.selectById(msgId);
        if (messageLog == null) {
            throw new RuntimeException("挂号消息不存在，无法处理");
        }
        if (Integer.valueOf(2).equals(messageLog.getStatus())) {
            return;
        }
        if (Integer.valueOf(3).equals(messageLog.getStatus())) {
            return;
        }
        if (hasActiveRegistration(scheduleId, patientId, null)) {
            markMessageFailed(messageLog, scheduleId);
            return;
        }

        // 这是消费者要执行的真实落盘逻辑
        DoctorScheduleEntity schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getAvailableCount() <= 0) {
            markMessageFailed(messageLog, scheduleId);
            return;
        }

        // 1. 扣减数据库库存
        schedule.setAvailableCount(schedule.getAvailableCount() - 1);
        doctorScheduleMapper.updateById(schedule);

        // 2. 生成挂号单
        RegistrationEntity reg = new RegistrationEntity();
        reg.setRegistrationNo("REG" + System.currentTimeMillis() + patientId);
        reg.setPatientId(patientId);
        reg.setDoctorId(schedule.getDoctorId());
        reg.setDepartmentId(schedule.getDepartmentId());
        reg.setScheduleId(scheduleId);
        reg.setVisitDate(schedule.getScheduleDate());
        reg.setTimeSlot(schedule.getTimeSlot());
        reg.setStatus("UNPAID"); // 待缴费
        reg.setFeeAmount(schedule.getFeeAmount());
        reg.setRegisteredAt(LocalDateTime.now());
        reg.setCreatedAt(LocalDateTime.now());
        reg.setUpdatedAt(LocalDateTime.now());
        
        // 插入挂号单，此时不加入接诊队列(VisitQueue)，等缴费成功后再分配 queueNo 并入队
        registrationMapper.insert(reg);

        messageLogMapper.update(null,
                new LambdaUpdateWrapper<RegistrationMessageLogEntity>()
                        .eq(RegistrationMessageLogEntity::getMsgId, messageLog.getMsgId())
                        .set(RegistrationMessageLogEntity::getStatus, 2)
                        .set(RegistrationMessageLogEntity::getUpdateTime, LocalDateTime.now()));
    }

    private void markMessageFailed(RegistrationMessageLogEntity messageLog, Long scheduleId) {
        messageLogMapper.update(null,
                new LambdaUpdateWrapper<RegistrationMessageLogEntity>()
                        .eq(RegistrationMessageLogEntity::getMsgId, messageLog.getMsgId())
                        .set(RegistrationMessageLogEntity::getStatus, 3)
                        .set(RegistrationMessageLogEntity::getRetryCount,
                                (messageLog.getRetryCount() == null ? 0 : messageLog.getRetryCount()) + 1)
                        .set(RegistrationMessageLogEntity::getUpdateTime, LocalDateTime.now()));

        try {
            String stockKey = "schedule:stock:" + scheduleId;
            redisTemplate.opsForValue().increment(stockKey);
        } catch (Exception ex) {
            log.error("挂号消息失败后回补 Redis 库存失败, scheduleId={}, msgId={}", scheduleId, messageLog.getMsgId(), ex);
        }
    }

    private void ensureNoDuplicateRegistration(Long scheduleId, Long patientId) {
        if (hasActiveRegistration(scheduleId, patientId, null)) {
            throw new BusinessException(400, "当前排班您已挂号，请勿重复提交");
        }
        if (hasPendingRegistrationMessage(scheduleId, patientId)) {
            throw new BusinessException(400, "当前排班挂号申请正在处理中，请勿重复提交");
        }
    }

    private boolean hasActiveRegistration(Long scheduleId, Long patientId, Long excludeRegistrationId) {
        LambdaQueryWrapper<RegistrationEntity> wrapper = new LambdaQueryWrapper<RegistrationEntity>()
                .eq(RegistrationEntity::getScheduleId, scheduleId)
                .eq(RegistrationEntity::getPatientId, patientId)
                .ne(RegistrationEntity::getStatus, "CANCELLED")
                .eq(RegistrationEntity::getDeleted, false);
        if (excludeRegistrationId != null) {
            wrapper.ne(RegistrationEntity::getId, excludeRegistrationId);
        }
        return registrationMapper.selectCount(wrapper) > 0;
    }

    private boolean hasPendingRegistrationMessage(Long scheduleId, Long patientId) {
        LambdaQueryWrapper<RegistrationMessageLogEntity> wrapper = new LambdaQueryWrapper<RegistrationMessageLogEntity>()
                .eq(RegistrationMessageLogEntity::getScheduleId, scheduleId)
                .eq(RegistrationMessageLogEntity::getPatientId, patientId)
                .in(RegistrationMessageLogEntity::getStatus, 0, 1);
        return messageLogMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Page<RegistrationEntity> getMyRegistrations(Long patientId, int pageNo, int pageSize) {
        Page<RegistrationEntity> page = new Page<>(pageNo, pageSize);
        QueryWrapper<RegistrationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("patient_id", patientId);
        wrapper.ne("status", "CANCELLED");
        wrapper.orderByDesc("created_at");
        Page<RegistrationEntity> result = registrationMapper.selectPage(page, wrapper);
        enrichRegistrationNames(result.getRecords());
        return result;
    }

    @Override
    public Page<RegistrationEntity> getAllRegistrations(int pageNo, int pageSize) {
        Page<RegistrationEntity> page = new Page<>(pageNo, pageSize);
        QueryWrapper<RegistrationEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        Page<RegistrationEntity> result = registrationMapper.selectPage(page, wrapper);
        enrichRegistrationNames(result.getRecords());
        return result;
    }

    @Override
    public RegistrationEntity getRegistrationById(Long id) {
        RegistrationEntity registration = registrationMapper.selectById(id);
        enrichRegistrationNames(Collections.singletonList(registration));
        return registration;
    }

    private void enrichRegistrationNames(List<RegistrationEntity> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return;
        }
        for (RegistrationEntity registration : registrations) {
            if (registration == null) {
                continue;
            }
            PatientEntity patient = patientMapper.selectById(registration.getPatientId());
            if (patient != null) {
                registration.setPatientName(patient.getName());
            }
            DoctorEntity doctor = doctorMapper.selectById(registration.getDoctorId());
            if (doctor != null) {
                registration.setDoctorName(doctor.getName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id, Long patientId) {
        RegistrationEntity reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new BusinessException(400, "挂号单不存在");
        }
        if (patientId != null && !reg.getPatientId().equals(patientId)) {
            throw new BusinessException(403, "无权操作此挂号单");
        }
        if (!"UNPAID".equals(reg.getStatus()) && !"PAID".equals(reg.getStatus()) && !"PENDING".equals(reg.getStatus())) {
            throw new BusinessException(400, "当前状态不可退号");
        }
        if ("PAID".equals(reg.getStatus())) {
            refundPaidRegistration(reg);
        }

        // 修改状态
        reg.setStatus("CANCELLED");
        reg.setCancelReason("患者主动退号");
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(reg);

        // 回滚排班库存
        DoctorScheduleEntity schedule = doctorScheduleMapper.selectById(reg.getScheduleId());
        if (schedule != null) {
            schedule.setAvailableCount(schedule.getAvailableCount() + 1);
            doctorScheduleMapper.updateById(schedule);
        }

        // 回滚 Redis 库存
        String stockKey = "schedule:stock:" + reg.getScheduleId();
        redisTemplate.opsForValue().increment(stockKey);
    }

    private void refundPaidRegistration(RegistrationEntity reg) {
        List<PaymentItemEntity> paidItems = paymentItemMapper.selectList(
                new LambdaQueryWrapper<PaymentItemEntity>()
                        .eq(PaymentItemEntity::getItemType, "REGISTRATION")
                        .eq(PaymentItemEntity::getBizId, reg.getId())
                        .eq(PaymentItemEntity::getDeleted, false)
                        .eq(PaymentItemEntity::getStatus, "PAID")
        );
        if (paidItems == null || paidItems.isEmpty()) {
            throw new BusinessException(400, "挂号单已支付，但未找到可退款的支付记录");
        }

        Long operatorId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        for (PaymentItemEntity item : paidItems) {
            PaymentOrderEntity order = paymentOrderMapper.selectById(item.getPaymentOrderId());
            if (order == null) {
                throw new BusinessException(400, "支付订单不存在，无法完成退号退款");
            }
            createRefundRecord(order.getId(), item.getAmount(), operatorId, now);
            item.setStatus("REFUNDED");
            item.setUpdatedAt(now);
            item.setUpdatedBy(operatorId);
            paymentItemMapper.updateById(item);
            refreshOrderRefundStatus(order.getId(), operatorId, now);
        }
    }

    private void createRefundRecord(Long paymentOrderId, BigDecimal refundAmount, Long operatorId, LocalDateTime now) {
        RefundRecordEntity refundRecord = new RefundRecordEntity();
        refundRecord.setPaymentOrderId(paymentOrderId);
        refundRecord.setRefundNo("REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        refundRecord.setRefundAmount(refundAmount);
        refundRecord.setRefundReason("患者主动退号");
        refundRecord.setOperatorId(operatorId);
        refundRecord.setRefundTime(now);
        refundRecord.setStatus("SUCCESS");
        refundRecord.setCreatedAt(now);
        refundRecord.setUpdatedAt(now);
        refundRecord.setCreatedBy(operatorId);
        refundRecord.setUpdatedBy(operatorId);
        refundRecord.setDeleted(false);
        refundRecordMapper.insert(refundRecord);
    }

    private void refreshOrderRefundStatus(Long orderId, Long operatorId, LocalDateTime now) {
        List<PaymentItemEntity> orderItems = paymentItemMapper.selectList(
                new LambdaQueryWrapper<PaymentItemEntity>()
                        .eq(PaymentItemEntity::getPaymentOrderId, orderId)
                        .eq(PaymentItemEntity::getDeleted, false)
        );
        boolean hasRefunded = orderItems.stream().anyMatch(item -> "REFUNDED".equals(item.getStatus()));
        boolean allRefunded = !orderItems.isEmpty() && orderItems.stream().allMatch(item -> "REFUNDED".equals(item.getStatus()));
        if (!hasRefunded) {
            return;
        }

        PaymentOrderEntity order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        order.setPayStatus(allRefunded ? "REFUNDED" : "PARTIAL_REFUND");
        order.setUpdatedAt(now);
        order.setUpdatedBy(operatorId);
        paymentOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id, Long patientId) {
        RegistrationEntity reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new RuntimeException("挂号单不存在");
        }
        if (patientId != null && !reg.getPatientId().equals(patientId)) {
            throw new RuntimeException("无权操作此挂号单");
        }
        if (!"PAID".equals(reg.getStatus())) {
            throw new RuntimeException("挂号单未缴费或已报到");
        }
        if (reg.getVisitDate() == null || !reg.getVisitDate().equals(LocalDate.now())) {
            throw new RuntimeException("只能在就诊当日签到");
        }

        // 修改挂号单状态
        reg.setStatus("IN_PROGRESS");
        reg.setUpdatedAt(LocalDateTime.now());

        // 入队
        QueryWrapper<VisitQueueEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", reg.getDoctorId());
        wrapper.orderByDesc("queue_no");
        wrapper.last("LIMIT 1");
        VisitQueueEntity lastQueue = visitQueueMapper.selectOne(wrapper);
        int currentQueueNo = (lastQueue == null || lastQueue.getQueueNo() == null) ? 1 : lastQueue.getQueueNo() + 1;

        reg.setQueueNo(currentQueueNo);
        registrationMapper.updateById(reg);

        VisitQueueEntity vq = new VisitQueueEntity();
        vq.setRegistrationId(reg.getId());
        vq.setDoctorId(reg.getDoctorId());
        vq.setQueueNo(currentQueueNo);
        vq.setQueueStatus("WAITING");
        vq.setCreatedAt(LocalDateTime.now());
        vq.setUpdatedAt(LocalDateTime.now());
        visitQueueMapper.insert(vq);
    }
}
