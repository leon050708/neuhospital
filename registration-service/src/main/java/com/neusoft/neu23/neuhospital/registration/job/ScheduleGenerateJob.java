package com.neusoft.neu23.neuhospital.registration.job;

import com.neusoft.neu23.neuhospital.registration.config.RegistrationScheduleProperties;
import com.neusoft.neu23.neuhospital.registration.dto.ScheduleBatchGenerateReq;
import com.neusoft.neu23.neuhospital.registration.service.ScheduleGenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduleGenerateJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleGenerateJob.class);

    private final ScheduleGenerateService scheduleGenerateService;
    private final RegistrationScheduleProperties scheduleProperties;

    public ScheduleGenerateJob(ScheduleGenerateService scheduleGenerateService,
                                 RegistrationScheduleProperties scheduleProperties) {
        this.scheduleGenerateService = scheduleGenerateService;
        this.scheduleProperties = scheduleProperties;
    }

    @Scheduled(cron = "${app.registration.auto-generate-cron:0 0 23 * * SUN}")
    public void autoGenerateWeeklySchedules() {
        if (!scheduleProperties.isAutoGenerateEnabled()) {
            return;
        }
        try {
            ScheduleBatchGenerateReq req = new ScheduleBatchGenerateReq();
            req.setDays(scheduleProperties.getAdvanceDays());
            var result = scheduleGenerateService.generateFromTemplates(req);
            log.info("Auto schedule generation finished: created={}, skipped={}, range={}~{}",
                    result.getCreatedCount(), result.getSkippedCount(), result.getStartDate(), result.getEndDate());
        } catch (Exception ex) {
            log.warn("Auto schedule generation failed: {}", ex.getMessage());
        }
    }
}
