package com.neusoft.neu23.neuhospital.integration.registration;

import java.util.List;

public interface RegistrationGatewayClient {

    List<RemoteScheduleSummary> getSchedulesByDepartment(Long departmentId);

    String quickRegister(Long patientId, Long scheduleId);
}
