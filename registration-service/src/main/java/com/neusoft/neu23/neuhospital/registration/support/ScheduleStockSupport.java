package com.neusoft.neu23.neuhospital.registration.support;

import com.neusoft.neu23.neuhospital.registration.entity.DoctorScheduleEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScheduleStockSupport {

    private final StringRedisTemplate redisTemplate;

    public ScheduleStockSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void initializeStock(DoctorScheduleEntity schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        int available = schedule.getAvailableCount() == null ? 0 : schedule.getAvailableCount();
        redisTemplate.opsForValue().set(stockKey(schedule.getId()), String.valueOf(Math.max(available, 0)));
    }

    public void syncStock(DoctorScheduleEntity schedule) {
        initializeStock(schedule);
    }

    public static String stockKey(Long scheduleId) {
        return "schedule:stock:" + scheduleId;
    }
}
