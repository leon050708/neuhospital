package com.neusoft.neu23.neuhospital.registration;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationMessageLogEntity;
import com.neusoft.neu23.neuhospital.registration.mapper.RegistrationMessageLogMapper;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RegistrationConcurrentTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private RegistrationMessageLogMapper messageLogMapper;

    @Test
    public void quickRegister_shouldPersistExactlyStockMessageLogsUnderConcurrency() throws InterruptedException {
        Long scheduleId = System.currentTimeMillis();
        long patientIdBase = scheduleId * 1000;
        int threadCount = 300;
        int stock = 20;
        String stockKey = "schedule:stock:" + scheduleId;
        String limitKey = "rate_limit:registration:" + scheduleId;

        cleanup(stockKey, limitKey);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        redisTemplate.delete(limitKey);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Set<String> acceptedMsgIds = ConcurrentHashMap.newKeySet();
        Set<Long> acceptedPatientIds = ConcurrentHashMap.newKeySet();
        ConcurrentLinkedQueue<String> failureMessages = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<RegistrationMessageLogEntity> insertedLogs = new ConcurrentLinkedQueue<>();

        when(messageLogMapper.insertWithJsonPayload(any(RegistrationMessageLogEntity.class))).thenAnswer(invocation -> {
            RegistrationMessageLogEntity log = invocation.getArgument(0);
            insertedLogs.add(log);
            return 1;
        });

        try {
            for (int i = 0; i < threadCount; i++) {
                final long patientId = patientIdBase + i;
                executorService.execute(() -> {
                    try {
                        startLatch.await();

                        RegistrationCreateReq req = new RegistrationCreateReq();
                        req.setScheduleId(scheduleId);
                        req.setPatientId(patientId);

                        String msgId = registrationService.quickRegister(req);
                        acceptedMsgIds.add(msgId);
                        acceptedPatientIds.add(patientId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        if (failureMessages.size() < 5) {
                            failureMessages.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                        failCount.incrementAndGet();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            System.out.println("所有线程准备完毕，开始抢号...");
            long startTime = System.currentTimeMillis();
            startLatch.countDown();
            endLatch.await();
            long endTime = System.currentTimeMillis();

            String remainingStock = redisTemplate.opsForValue().get(stockKey);

            System.out.println("============================");
            System.out.println("抢号总耗时: " + (endTime - startTime) + "ms");
            System.out.println("抢号成功人数 (预期 " + stock + "): " + successCount.get());
            System.out.println("抢号失败人数 (预期 " + (threadCount - stock) + "): " + failCount.get());
            System.out.println("消息表插入次数 (预期 " + stock + "): " + insertedLogs.size());
            System.out.println("Redis剩余库存 (预期 0): " + remainingStock);
            System.out.println("失败样例: " + failureMessages);
            System.out.println("============================");

            assertEquals(stock, successCount.get(), "成功数必须严格等于初始库存");
            assertEquals(threadCount - stock, failCount.get(), "失败数必须严格等于并发数减库存");
            assertEquals(stock, acceptedMsgIds.size(), "成功请求返回的 msgId 必须一一对应");
            assertEquals(stock, acceptedPatientIds.size(), "成功患者数必须与成功请求数一致");
            assertEquals(stock, insertedLogs.size(), "quickRegister 当前只写 message_log，所以应严格写出 stock 条消息");
            assertTrue(insertedLogs.stream().allMatch(log -> scheduleId.equals(log.getScheduleId())), "所有消息必须绑定同一个排班");
            assertEquals(stock, insertedLogs.stream().map(RegistrationMessageLogEntity::getPatientId).distinct().count(),
                    "写出的消息必须对应 stock 个不同患者");
            assertEquals("0", remainingStock, "成功抢完后 Redis 库存应归零");
        } finally {
            executorService.shutdownNow();
            cleanup(stockKey, limitKey);
        }
    }

    private void cleanup(String stockKey, String limitKey) {
        redisTemplate.delete(stockKey);
        redisTemplate.delete(limitKey);
    }
}
