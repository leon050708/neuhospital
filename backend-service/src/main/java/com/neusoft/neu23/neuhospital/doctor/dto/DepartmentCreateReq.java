package com.neusoft.neu23.neuhospital.doctor.dto;

public class DepartmentCreateReq {
    private String deptCode;
    private String deptName;
    private String deptType;
    private String description;

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getDeptType() { return deptType; }
    public void setDeptType(String deptType) { this.deptType = deptType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
