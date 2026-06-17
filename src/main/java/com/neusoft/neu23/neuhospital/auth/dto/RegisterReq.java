package com.neusoft.neu23.neuhospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "患者账号注册请求")
public class RegisterReq {
    @Schema(description = "登录密码", example = "123456")
    private String password;
    @Schema(description = "真实姓名", example = "张三")
    private String realName;
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "身份证号", example = "210102199901011234")
    private String idCard;
    @Schema(description = "性别", example = "男")
    private String gender;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
