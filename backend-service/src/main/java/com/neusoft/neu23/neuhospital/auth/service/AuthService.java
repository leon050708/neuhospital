package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.dto.LoginRequest;
import com.neusoft.neu23.neuhospital.auth.dto.RefreshTokenRequest;
import com.neusoft.neu23.neuhospital.auth.vo.LoginResponse;
import com.neusoft.neu23.neuhospital.auth.vo.RefreshTokenResponse;

/**
 * Authentication use cases. Concrete implementation will be added with user lookup later.
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken);

    void register(com.neusoft.neu23.neuhospital.auth.dto.RegisterReq req);

    com.neusoft.neu23.neuhospital.auth.vo.UserProfileResponse getCurrentUser(String accessToken);
}
