package com.neusoft.neu23.neuhospital.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public static Long getCurrentPatientId() {
        CustomUserDetails user = requireCurrentUser();
        if (!"PATIENT".equals(user.getUserType())) {
            throw new IllegalStateException("当前登录账号不是患者");
        }
        if (user.getBizId() == null) {
            throw new IllegalStateException("当前患者账号未绑定业务主键");
        }
        return user.getBizId();
    }

    public static Long getCurrentDoctorId() {
        CustomUserDetails user = requireCurrentUser();
        if (!"DOCTOR".equals(user.getUserType())) {
            throw new IllegalStateException("当前登录账号不是医生");
        }
        if (user.getBizId() == null) {
            throw new IllegalStateException("当前医生账号未绑定业务主键");
        }
        return user.getBizId();
    }

    private static CustomUserDetails requireCurrentUser() {
        CustomUserDetails user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("未登录或会话已过期");
        }
        return user;
    }
}
