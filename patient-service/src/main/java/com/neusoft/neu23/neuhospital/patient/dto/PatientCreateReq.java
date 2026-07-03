package com.neusoft.neu23.neuhospital.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "新增患者请求")
public class PatientCreateReq {
    @Schema(description = "患者姓名", example = "王敏")
    private String name;
    @Schema(description = "性别", example = "女")
    private String gender;
    @Schema(description = "出生日期", example = "1998-03-12")
    private LocalDate birthDate;
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "身份证号", example = "210102199803121234")
    private String idCard;
    @Schema(description = "血型", example = "A")
    private String bloodType;
    @Schema(description = "过敏史摘要")
    private String allergySummary;
    @Schema(description = "病史摘要")
    private String historySummary;
    @Schema(description = "紧急联系人", example = "王建国")
    private String emergencyContact;
    @Schema(description = "紧急联系人电话", example = "13900139000")
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
