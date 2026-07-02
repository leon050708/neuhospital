package com.neusoft.neu23.neuhospital.doctor.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "医生档案响应")
public class DoctorVO {
    @Schema(description = "医生主键 ID", example = "10")
    private Long id;
    @Schema(description = "医生工号", example = "D20240001")
    private String doctorNo;
    @Schema(description = "医生姓名", example = "李华")
    private String name;
    @Schema(description = "性别", example = "男")
    private String gender;
    @Schema(description = "职称", example = "主任医师")
    private String title;
    @Schema(description = "所属科室 ID", example = "3")
    private Long departmentId;
    @Schema(description = "所属科室名称", example = "心内科")
    private String departmentName; // 追加科室名称方便前端展示
    @Schema(description = "医生简介")
    private String introduction;
    @Schema(description = "擅长方向")
    private String specialty;
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;
    @Schema(description = "状态", example = "ENABLED")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDoctorNo() { return doctorNo; }
    public void setDoctorNo(String doctorNo) { this.doctorNo = doctorNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
