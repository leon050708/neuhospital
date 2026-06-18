package com.neusoft.neu23.neuhospital.registration.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QueueItemVO {
    private Long id; // visit_queue.id
    private Long registrationId;
    private Long patientId;
    private Integer queueNo;
    private String queueStatus; // WAITING, CALLED, SKIPPED, FINISHED
    private LocalDateTime calledAt;
    
    // 从 registration 中获取的辅助信息
    // 假设联表查询或者服务层聚合，为简化这里只写几个关键字段，真实场景可调用 patientService 获取患者姓名等
    private String registrationNo;
    private LocalDateTime registeredAt;
}
