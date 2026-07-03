package com.neusoft.neu23.neuhospital.doctor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "新增医生请求")
public class DoctorCreateReq {
    @Schema(description = "医生姓名", example = "李华")
    private String name;
    @Schema(description = "性别", example = "男")
    private String gender;
    @Schema(description = "职称", example = "主任医师")
    private String title;
    @Schema(description = "所属科室 ID", example = "3")
    private Long departmentId;
    @Schema(description = "医生简介")
    private String introduction;
    @Schema(description = "擅长方向")
    private String specialty;
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
