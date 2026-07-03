package com.neusoft.neu23.neuhospital.doctor.vo;

public class DepartmentVO {
    private Long id;
    private String deptCode;
    private String deptName;
    private String deptType;
    private String description;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getDeptType() { return deptType; }
    public void setDeptType(String deptType) { this.deptType = deptType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
