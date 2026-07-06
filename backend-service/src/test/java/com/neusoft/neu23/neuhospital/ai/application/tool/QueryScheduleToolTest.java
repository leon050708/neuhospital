package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.integration.registration.RegistrationGatewayClient;
import com.neusoft.neu23.neuhospital.integration.registration.RemoteScheduleSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryScheduleToolTest {

    @Test
    void shouldFilterSchedulesByRelativeDateAndTimeSlot() {
        RegistrationGatewayClient registrationGatewayClient = mock(RegistrationGatewayClient.class);
        QueryScheduleTool tool = new QueryScheduleTool(registrationGatewayClient);

        LocalDate today = LocalDate.now();
        when(registrationGatewayClient.getSchedulesByDepartment(12L)).thenReturn(List.of(
                schedule(1L, "张医生", today.plusDays(1), "上午", 3, "ENABLED"),
                schedule(2L, "李医生", today.plusDays(1), "下午", 5, "ENABLED"),
                schedule(3L, "王医生", today.plusDays(2), "上午", 4, "ENABLED")
        ));

        String result = tool.querySchedule().apply(new QueryScheduleTool.Request(12L, "明天", "上午"));

        assertEquals(String.format("排班ID: 1, 医生: 张医生, 日期: %s, 时段: 上午, 剩余号源: 3", today.plusDays(1)), result);
    }

    @Test
    void shouldReturnNoMatchMessageWhenRequestedDateHasNoSchedules() {
        RegistrationGatewayClient registrationGatewayClient = mock(RegistrationGatewayClient.class);
        QueryScheduleTool tool = new QueryScheduleTool(registrationGatewayClient);

        LocalDate today = LocalDate.now();
        when(registrationGatewayClient.getSchedulesByDepartment(12L)).thenReturn(List.of(
                schedule(1L, "张医生", today.plusDays(1), "上午", 3, "ENABLED")
        ));

        String result = tool.querySchedule().apply(new QueryScheduleTool.Request(12L, "后天", "下午"));

        assertEquals("该科室近期无符合条件的排班", result);
    }

    private RemoteScheduleSummary schedule(Long id,
                                           String doctorName,
                                           LocalDate scheduleDate,
                                           String timeSlot,
                                           Integer availableCount,
                                           String status) {
        RemoteScheduleSummary summary = new RemoteScheduleSummary();
        summary.setId(id);
        summary.setDoctorName(doctorName);
        summary.setScheduleDate(scheduleDate);
        summary.setTimeSlot(timeSlot);
        summary.setAvailableCount(availableCount);
        summary.setStatus(status);
        return summary;
    }
}
