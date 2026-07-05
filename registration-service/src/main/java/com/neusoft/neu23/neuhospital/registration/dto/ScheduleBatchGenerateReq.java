package com.neusoft.neu23.neuhospital.registration.dto;

import java.time.LocalDate;

public class ScheduleBatchGenerateReq {
    /** 生成天数，默认取配置 app.registration.advance-days */
    private Integer days;
    /** 起始日期，默认今天 */
    private LocalDate startDate;
    private Long doctorId;
    private Long departmentId;

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
