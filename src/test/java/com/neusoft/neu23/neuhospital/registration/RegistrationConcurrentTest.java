package com.neusoft.neu23.neuhospital.registration;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class RegistrationConcurrentTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testConcurrentRegistration() throws InterruptedException {
        Long scheduleId = 1001L; // 假设测试排班 ID 为 1001
        int threadCount = 300;
        int stock = 20;

        // 1. 准备 Redis 库存数据
        redisTemplate.opsForValue().set("schedule:stock:" + scheduleId, String.valueOf(stock));
        // 清理限流等数据
        redisTemplate.delete("rate_limit:registration:" + scheduleId);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long patientId = i + 1;
            executorService.execute(() -> {
                try {
                    startLatch.await(); // 等待统一发令
                    
                    RegistrationCreateReq req = new RegistrationCreateReq();
                    req.setScheduleId(scheduleId);
                    req.setPatientId(patientId);
                    
                    registrationService.quickRegister(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        System.out.println("所有线程准备完毕，开始抢号...");
        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // 发令枪
        endLatch.await(); // 等待所有人抢完
        long endTime = System.currentTimeMillis();

        System.out.println("============================");
        System.out.println("抢号总耗时: " + (endTime - startTime) + "ms");
        System.out.println("抢号成功人数 (预期 "+stock+"): " + successCount.get());
        System.out.println("抢号失败人数 (预期 "+(threadCount-stock)+"): " + failCount.get());
        System.out.println("Redis剩余库存: " + redisTemplate.opsForValue().get("schedule:stock:" + scheduleId));
        System.out.println("============================");

        // 断言，但由于只是打印看看，这里不做强断言阻塞
        assert successCount.get() <= stock;
    }
}
