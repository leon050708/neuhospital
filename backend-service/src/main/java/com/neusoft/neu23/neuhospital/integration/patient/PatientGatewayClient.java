package com.neusoft.neu23.neuhospital.integration.patient;

public interface PatientGatewayClient {

    RemotePatientProfile getPatientById(Long patientId);

    void updatePatient(Long patientId, RemotePatientUpdateRequest request);
}
