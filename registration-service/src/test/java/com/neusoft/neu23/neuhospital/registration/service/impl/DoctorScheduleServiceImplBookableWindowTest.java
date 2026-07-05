package com.neusoft.neu23.neuhospital.registration.service.impl;

import com.neusoft.neu23.neuhospital.registration.config.RegistrationScheduleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorScheduleServiceImplBookableWindowTest {

    private RegistrationScheduleProperties scheduleProperties;

    @BeforeEach
    void setUp() {
        scheduleProperties = new RegistrationScheduleProperties();
        scheduleProperties.setAdvanceDays(7);
    }

    @Test
    void isBookableScheduleDate_shouldAllowSevenDayWindowIncludingToday() {
        DoctorScheduleServiceImpl service = new DoctorScheduleServiceImpl(
                null, null, null, scheduleProperties, null);
        LocalDate today = LocalDate.now();

        assertTrue(service.isBookableScheduleDate(today));
        assertTrue(service.isBookableScheduleDate(today.plusDays(6)));
        assertFalse(service.isBookableScheduleDate(today.minusDays(1)));
        assertFalse(service.isBookableScheduleDate(today.plusDays(7)));
    }
}
