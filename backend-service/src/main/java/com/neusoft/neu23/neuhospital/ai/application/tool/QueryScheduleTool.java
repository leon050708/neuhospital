package com.neusoft.neu23.neuhospital.ai.application.tool;

import com.neusoft.neu23.neuhospital.integration.registration.RegistrationGatewayClient;
import com.neusoft.neu23.neuhospital.integration.registration.RemoteScheduleSummary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class QueryScheduleTool {

    private final RegistrationGatewayClient registrationGatewayClient;

    public QueryScheduleTool(RegistrationGatewayClient registrationGatewayClient) {
        this.registrationGatewayClient = registrationGatewayClient;
    }

    public record Request(Long departmentId, String scheduleDate, String timeSlot) {}

    @Bean
    @Description("查询某科室近期的排班信息。必须提供正确的 departmentId。scheduleDate 可选，支持 今天/明天/后天 或 YYYY-MM-DD。timeSlot 可选，支持 上午/下午/晚上。返回包含 scheduleId 和 剩余号源 availableCount 的信息。")
    public Function<Request, String> querySchedule() {
        return request -> {
            if (request.departmentId() == null) {
                return "必须提供 departmentId";
            }

            LocalDate requestedDate;
            try {
                requestedDate = resolveScheduleDate(request.scheduleDate());
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
            String requestedTimeSlot = normalizeTimeSlot(request.timeSlot());

            var schedules = registrationGatewayClient.getSchedulesByDepartment(request.departmentId());
            if (schedules == null || schedules.isEmpty()) {
                return "该科室近期无排班";
            }

            List<RemoteScheduleSummary> filteredSchedules = schedules.stream()
                    .filter(this::isAvailable)
                    .filter(schedule -> matchesRequestedDate(schedule, requestedDate))
                    .filter(schedule -> matchesRequestedTimeSlot(schedule, requestedTimeSlot))
                    .sorted(Comparator
                            .comparing(RemoteScheduleSummary::getScheduleDate, Comparator.nullsLast(LocalDate::compareTo))
                            .thenComparing(schedule -> timeSlotOrder(schedule.getTimeSlot()))
                            .thenComparing(RemoteScheduleSummary::getId, Comparator.nullsLast(Long::compareTo)))
                    .collect(Collectors.toList());

            if (filteredSchedules.isEmpty()) {
                return "该科室近期无符合条件的排班";
            }

            return filteredSchedules.stream()
                    .map(s -> String.format("排班ID: %d, 医生: %s, 日期: %s, 时段: %s, 剩余号源: %d",
                            s.getId(), s.getDoctorName(), s.getScheduleDate(), s.getTimeSlot(), s.getAvailableCount()))
                    .collect(Collectors.joining("\n"));
        };
    }

    private boolean isAvailable(RemoteScheduleSummary schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getAvailableCount() == null || schedule.getAvailableCount() <= 0) {
            return false;
        }
        String status = schedule.getStatus();
        return "ENABLED".equalsIgnoreCase(status) || "AVAILABLE".equalsIgnoreCase(status);
    }

    private boolean matchesRequestedDate(RemoteScheduleSummary schedule, LocalDate requestedDate) {
        if (requestedDate == null) {
            return true;
        }
        return requestedDate.equals(schedule.getScheduleDate());
    }

    private boolean matchesRequestedTimeSlot(RemoteScheduleSummary schedule, String requestedTimeSlot) {
        if (!StringUtils.hasText(requestedTimeSlot)) {
            return true;
        }
        return requestedTimeSlot.equals(normalizeTimeSlot(schedule.getTimeSlot()));
    }

    private LocalDate resolveScheduleDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }
        String normalized = rawDate.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        return switch (normalized) {
            case "今天", "today" -> today;
            case "明天", "tomorrow" -> today.plusDays(1);
            case "后天" -> today.plusDays(2);
            default -> parseIsoDate(rawDate.trim());
        };
    }

    private LocalDate parseIsoDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("scheduleDate 仅支持 今天/明天/后天 或 YYYY-MM-DD");
        }
    }

    private String normalizeTimeSlot(String rawTimeSlot) {
        if (!StringUtils.hasText(rawTimeSlot)) {
            return null;
        }
        String normalized = rawTimeSlot.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "am", "morning", "上午" -> "上午";
            case "pm", "afternoon", "下午" -> "下午";
            case "evening", "night", "晚上" -> "晚上";
            default -> rawTimeSlot.trim();
        };
    }

    private int timeSlotOrder(String timeSlot) {
        String normalized = normalizeTimeSlot(timeSlot);
        if ("上午".equals(normalized)) {
            return 1;
        }
        if ("下午".equals(normalized)) {
            return 2;
        }
        if ("晚上".equals(normalized)) {
            return 3;
        }
        return 9;
    }
}
