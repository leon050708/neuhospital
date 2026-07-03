package com.neusoft.neu23.neuhospital.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getCurrentUserType() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUserType() : null;
    }

    public static Long getCurrentBizId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getBizId() : null;
    }

    public static Long getCurrentPatientId() {
        CustomUserDetails user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("未登录或会话已过期");
        }
        if (!"PATIENT".equals(user.getUserType())) {
            throw new IllegalStateException("当前登录账号不是患者");
        }
        if (user.getBizId() == null) {
            throw new IllegalStateException("当前患者账号未绑定业务主键");
        }
        return user.getBizId();
    }

    public static Long getCurrentDoctorId() {
        CustomUserDetails user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("未登录或会话已过期");
        }
        if (!"DOCTOR".equals(user.getUserType())) {
            throw new IllegalStateException("当前登录账号不是医生");
        }
        if (user.getBizId() == null) {
            throw new IllegalStateException("当前医生账号未绑定业务主键");
        }
        return user.getBizId();
    }
}
