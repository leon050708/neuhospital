package com.neusoft.neu23.neuhospital.integration.registration;

import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.integration.config.DomainIntegrationProperties;
import com.neusoft.neu23.neuhospital.integration.support.IntegrationRequestHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class HttpRegistrationGatewayClient implements RegistrationGatewayClient {

    private static final ParameterizedTypeReference<Result<PageResult<RemoteScheduleSummary>>> SCHEDULE_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final IntegrationRequestHeaders requestHeaders;

    public HttpRegistrationGatewayClient(DomainIntegrationProperties properties,
                                         IntegrationRequestHeaders requestHeaders) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getRegistrationServiceBaseUrl())
                .build();
        this.requestHeaders = requestHeaders;
    }

    @Override
    public List<RemoteScheduleSummary> getSchedulesByDepartment(Long departmentId) {
        Result<PageResult<RemoteScheduleSummary>> result = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/schedules")
                        .queryParam("pageNo", 1)
                        .queryParam("pageSize", 50)
                        .queryParam("departmentId", departmentId)
                        .build())
                .headers(requestHeaders::apply)
                .retrieve()
                .body(SCHEDULE_PAGE_TYPE);
        if (result == null || result.getData() == null) {
            throw new BusinessException("查询排班失败: 空响应");
        }
        if (result.getCode() != null && result.getCode() != 200) {
            throw new BusinessException("查询排班失败: " + result.getMessage());
        }
        return result.getData().getRecords();
    }

    @Override
    public String quickRegister(Long patientId, Long scheduleId) {
        Map<String, Object> response = restClient.post()
                .uri("/api/registrations/quick")
                .headers(requestHeaders::apply)
                .body(Map.of("patientId", patientId, "scheduleId", scheduleId))
                .retrieve()
                .body(MAP_TYPE);
        if (response == null) {
            throw new BusinessException("挂号失败: 空响应");
        }
        Object success = response.get("success");
        if (success instanceof Boolean bool && !bool) {
            throw new BusinessException("挂号失败: " + response.get("message"));
        }
        Object msgId = response.get("msgId");
        return msgId == null ? "" : String.valueOf(msgId);
    }
}
