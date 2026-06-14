package com.neusoft.neu23.neuhospital.patient.dto;

import java.time.LocalDate;

public class PatientUpdateReq {
    private String name;
    private String phone;
    private String bloodType;
    private String allergySummary;
    private String historySummary;
    private String emergencyContact;
    private String emergencyPhone;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getAllergySummary() { return allergySummary; }
    public void setAllergySummary(String allergySummary) { this.allergySummary = allergySummary; }

    public String getHistorySummary() { return historySummary; }
    public void setHistorySummary(String historySummary) { this.historySummary = historySummary; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
