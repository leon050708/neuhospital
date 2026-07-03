package com.neusoft.neu23.neuhospital.integration.patient;

import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.integration.config.DomainIntegrationProperties;
import com.neusoft.neu23.neuhospital.integration.support.IntegrationRequestHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpPatientGatewayClient implements PatientGatewayClient {

    private static final ParameterizedTypeReference<Result<RemotePatientProfile>> PATIENT_RESULT_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final IntegrationRequestHeaders requestHeaders;

    public HttpPatientGatewayClient(DomainIntegrationProperties properties,
                                    IntegrationRequestHeaders requestHeaders) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getPatientServiceBaseUrl())
                .build();
        this.requestHeaders = requestHeaders;
    }

    @Override
    public RemotePatientProfile getPatientById(Long patientId) {
        Result<RemotePatientProfile> result = restClient.get()
                .uri("/api/patients/{id}", patientId)
                .headers(requestHeaders::apply)
                .retrieve()
                .body(PATIENT_RESULT_TYPE);
        return unwrap(result, "查询患者信息失败");
    }

    @Override
    public void updatePatient(Long patientId, RemotePatientUpdateRequest request) {
        Result<RemotePatientProfile> result = restClient.put()
                .uri("/api/patients/{id}", patientId)
                .headers(requestHeaders::apply)
                .body(request)
                .retrieve()
                .body(PATIENT_RESULT_TYPE);
        unwrap(result, "更新患者信息失败");
    }

    private RemotePatientProfile unwrap(Result<RemotePatientProfile> result, String defaultMessage) {
        if (result == null) {
            throw new BusinessException(defaultMessage + ": 空响应");
        }
        if (result.getCode() != null && result.getCode() != 200) {
            throw new BusinessException(defaultMessage + ": " + result.getMessage());
        }
        return result.getData();
    }
}
