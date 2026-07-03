package com.neusoft.neu23.neuhospital.patient.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新患者请求")
public class PatientUpdateReq {
    @Schema(description = "患者姓名", example = "王敏")
    private String name;
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
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
    @Schema(description = "状态", example = "ACTIVE")
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
