package com.neusoft.neu23.neuhospital.patient.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "患者档案响应")
public class PatientVO {
    @Schema(description = "患者主键 ID", example = "2001")
    private Long id;
    @Schema(description = "患者编号", example = "P20240001")
    private String patientNo;
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
    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientNo() { return patientNo; }
    public void setPatientNo(String patientNo) { this.patientNo = patientNo; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
