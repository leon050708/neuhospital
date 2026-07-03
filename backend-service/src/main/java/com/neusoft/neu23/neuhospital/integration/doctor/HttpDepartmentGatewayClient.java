package com.neusoft.neu23.neuhospital.integration.doctor;

import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.integration.config.DomainIntegrationProperties;
import com.neusoft.neu23.neuhospital.integration.support.IntegrationRequestHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class HttpDepartmentGatewayClient implements DepartmentGatewayClient {

    private static final ParameterizedTypeReference<Result<List<RemoteDepartmentSummary>>> DEPARTMENT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final IntegrationRequestHeaders requestHeaders;

    public HttpDepartmentGatewayClient(DomainIntegrationProperties properties,
                                       IntegrationRequestHeaders requestHeaders) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getDoctorServiceBaseUrl())
                .build();
        this.requestHeaders = requestHeaders;
    }

    @Override
    public List<RemoteDepartmentSummary> getAllDepartments() {
        Result<List<RemoteDepartmentSummary>> result = restClient.get()
                .uri("/api/departments")
                .headers(requestHeaders::apply)
                .retrieve()
                .body(DEPARTMENT_LIST_TYPE);
        if (result == null) {
            throw new BusinessException("查询科室列表失败: 空响应");
        }
        if (result.getCode() != null && result.getCode() != 200) {
            throw new BusinessException("查询科室列表失败: " + result.getMessage());
        }
        return result.getData();
    }
}
