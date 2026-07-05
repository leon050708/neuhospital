package com.neusoft.neu23.neuhospital.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationScheduleProperties {

    /**
     * 患者可预约的天数窗口（含当天）。例如 7 表示今天起共 7 个自然日。
     */
    private int advanceDays = 7;

    /**
     * 是否启用定时自动生成排班。
     */
    private boolean autoGenerateEnabled = false;

    /**
     * 定时任务 cron，默认每周日 23:00 生成下一周可预约窗口内的排班。
     */
    private String autoGenerateCron = "0 0 23 * * SUN";

    public int getAdvanceDays() {
        return advanceDays;
    }

    public void setAdvanceDays(int advanceDays) {
        this.advanceDays = advanceDays;
    }

    public boolean isAutoGenerateEnabled() {
        return autoGenerateEnabled;
    }

    public void setAutoGenerateEnabled(boolean autoGenerateEnabled) {
        this.autoGenerateEnabled = autoGenerateEnabled;
    }

    public String getAutoGenerateCron() {
        return autoGenerateCron;
    }

    public void setAutoGenerateCron(String autoGenerateCron) {
        this.autoGenerateCron = autoGenerateCron;
    }
}
