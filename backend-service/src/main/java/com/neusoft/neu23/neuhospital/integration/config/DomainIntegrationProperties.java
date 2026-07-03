package com.neusoft.neu23.neuhospital.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integration")
public class DomainIntegrationProperties {

    private String patientServiceBaseUrl = "http://localhost:10021";
    private String doctorServiceBaseUrl = "http://localhost:10022";
    private String registrationServiceBaseUrl = "http://localhost:10023";

    public String getPatientServiceBaseUrl() {
        return patientServiceBaseUrl;
    }

    public void setPatientServiceBaseUrl(String patientServiceBaseUrl) {
        this.patientServiceBaseUrl = patientServiceBaseUrl;
    }

    public String getDoctorServiceBaseUrl() {
        return doctorServiceBaseUrl;
    }

    public void setDoctorServiceBaseUrl(String doctorServiceBaseUrl) {
        this.doctorServiceBaseUrl = doctorServiceBaseUrl;
    }

    public String getRegistrationServiceBaseUrl() {
        return registrationServiceBaseUrl;
    }

    public void setRegistrationServiceBaseUrl(String registrationServiceBaseUrl) {
        this.registrationServiceBaseUrl = registrationServiceBaseUrl;
    }
}
