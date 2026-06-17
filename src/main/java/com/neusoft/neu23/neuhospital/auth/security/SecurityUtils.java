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
}
