package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RegistrationServiceImpl implements RegistrationService {

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

            // 3. Redis 库存预扣减
            String stockKey = "schedule:stock:" + scheduleId;
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock != null && stock < 0) {
                // 扣成负数，把库存加回来保证Redis数据整洁
                redisTemplate.opsForValue().increment(stockKey);
                throw new RuntimeException("手慢了，当前排班号源已满");
            }

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
            messageLogMapper.insert(log);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRegistrationMessage(String msgId, Long scheduleId, Long patientId) {
        // 这是消费者要执行的真实落盘逻辑
        DoctorScheduleEntity schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null || schedule.getAvailableCount() <= 0) {
            throw new RuntimeException("排班数据异常或已被占满，需要人工补偿回滚 Redis");
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
        reg.setStatus("PAID"); // 假设直接付款
        reg.setFeeAmount(schedule.getFeeAmount());
        reg.setRegisteredAt(LocalDateTime.now());
        reg.setCreatedAt(LocalDateTime.now());
        reg.setUpdatedAt(LocalDateTime.now());
        
        // 3. 计算排队号并加入接诊队列
        QueryWrapper<VisitQueueEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", schedule.getDoctorId());
        wrapper.orderByDesc("queue_no");
        wrapper.last("LIMIT 1");
        VisitQueueEntity lastQueue = visitQueueMapper.selectOne(wrapper);
        int currentQueueNo = (lastQueue == null || lastQueue.getQueueNo() == null) ? 1 : lastQueue.getQueueNo() + 1;
        reg.setQueueNo(currentQueueNo);
        registrationMapper.insert(reg);

        VisitQueueEntity vq = new VisitQueueEntity();
        vq.setRegistrationId(reg.getId());
        vq.setDoctorId(schedule.getDoctorId());
        vq.setQueueNo(currentQueueNo);
        vq.setQueueStatus("WAITING");
        vq.setCreatedAt(LocalDateTime.now());
        vq.setUpdatedAt(LocalDateTime.now());
        visitQueueMapper.insert(vq);
        
        // TODO: 状态扭转：在这里可以根据业务决定是否将 RegistrationMessageLog 更新为已消费，或者由别处处理。
    }
}
