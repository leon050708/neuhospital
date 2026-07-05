package com.neusoft.neu23.neuhospital.registration.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ScheduleGenerateResultVO {
    private LocalDate startDate;
    private LocalDate endDate;
    private int createdCount;
    private int skippedCount;
    private List<Long> createdScheduleIds = new ArrayList<>();

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<Long> getCreatedScheduleIds() {
        return createdScheduleIds;
    }

    public void setCreatedScheduleIds(List<Long> createdScheduleIds) {
        this.createdScheduleIds = createdScheduleIds;
    }
}
