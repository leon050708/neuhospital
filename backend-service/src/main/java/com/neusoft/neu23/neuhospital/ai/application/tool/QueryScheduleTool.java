package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.integration.registration.RegistrationGatewayClient;
import com.neusoft.neu23.neuhospital.integration.registration.RemoteScheduleSummary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class QueryScheduleTool {

    private final RegistrationGatewayClient registrationGatewayClient;

    public QueryScheduleTool(RegistrationGatewayClient registrationGatewayClient) {
        this.registrationGatewayClient = registrationGatewayClient;
    }

    public record Request(Long departmentId) {}

    @Bean
    @Description("查询某科室近期的排班信息。必须提供正确的 departmentId。返回包含 scheduleId 和 剩余号源 availableCount 的信息。")
    public Function<Request, String> querySchedule() {
        return request -> {
            if (request.departmentId() == null) {
                return "必须提供 departmentId";
            }
            // 查询所有时间、分页取前 50 条
            var schedules = registrationGatewayClient.getSchedulesByDepartment(request.departmentId());
            if (schedules == null || schedules.isEmpty()) {
                return "该科室近期无排班";
            }
            return schedules.stream()
                    .filter(s -> "ENABLED".equals(s.getStatus()) || "AVAILABLE".equals(s.getStatus())) // 根据实际状态值判断
                    .map(s -> String.format("排班ID: %d, 医生: %s, 日期: %s, 时段: %s, 剩余号源: %d",
                            s.getId(), s.getDoctorName(), s.getScheduleDate(), s.getTimeSlot(), s.getAvailableCount()))
                    .collect(Collectors.joining("\n"));
        };
    }
}
