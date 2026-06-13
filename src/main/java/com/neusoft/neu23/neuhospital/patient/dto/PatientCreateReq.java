package com.neusoft.neu23.neuhospital.patient.dto;

import java.time.LocalDate;

public class PatientCreateReq {
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String phone;
    private String idCard;
    private String bloodType;
    private String allergySummary;
    private String historySummary;
    private String emergencyContact;
    private String emergencyPhone;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

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
}
