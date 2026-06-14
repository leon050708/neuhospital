package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.dto.LoginRequest;
import com.neusoft.neu23.neuhospital.auth.dto.RefreshTokenRequest;
import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;
import com.neusoft.neu23.neuhospital.auth.security.JwtTokenProvider;
import com.neusoft.neu23.neuhospital.auth.security.JwtUserClaims;
import com.neusoft.neu23.neuhospital.auth.security.LoginSession;
import com.neusoft.neu23.neuhospital.auth.security.LoginSessionStore;
import com.neusoft.neu23.neuhospital.auth.security.RefreshSession;
import com.neusoft.neu23.neuhospital.auth.security.UserSessionBinding;
import com.neusoft.neu23.neuhospital.auth.vo.LoginResponse;
import com.neusoft.neu23.neuhospital.auth.vo.RefreshTokenResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal auth service implementing login, refresh and logout.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_ACTIVE = "ACTIVE";

    @org.springframework.beans.factory.annotation.Autowired
    private com.neusoft.neu23.neuhospital.patient.mapper.PatientMapper patientMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.neusoft.neu23.neuhospital.system.mapper.SysUserMapper sysUserMapper;

    private final AuthAccountProvider accountProvider;
    private final LoginSessionStore sessionStore;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthAccountProvider accountProvider,
                           LoginSessionStore sessionStore,
                           JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.accountProvider = accountProvider;
        this.sessionStore = sessionStore;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthAccount account = accountProvider.findByUsername(request.username());
        if (account == null || !STATUS_ENABLED.equals(account.status())) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        UserSessionBinding existingBinding = sessionStore.getUserSessionBinding(account.userId());
        if (existingBinding != null) {
            sessionStore.removeSession(existingBinding.sessionId());
            sessionStore.removeRefreshToken(existingBinding.refreshTokenId());
            sessionStore.removeUserSessionBinding(account.userId());
        }

        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        String refreshTokenId = "rt_" + UUID.randomUUID().toString().replace("-", "");
        String accessToken = tokenProvider.generateAccessToken(
                account.userId(), account.username(), account.role(), account.userType(), account.bizId(), sessionId
        );
        String refreshToken = tokenProvider.generateRefreshToken(
                account.userId(), account.role(), account.userType(), account.bizId(), sessionId, refreshTokenId
        );

        Instant now = Instant.now();
        sessionStore.saveSession(new LoginSession(
                sessionId, account.userId(), account.username(), account.userType(), account.role(),
                account.bizId(), refreshTokenId, STATUS_ONLINE, now
        ), tokenProvider.getAccessExpirationSeconds());
        sessionStore.saveRefreshToken(new RefreshSession(
                refreshTokenId, sessionId, account.userId(), account.userType(), account.role(),
                account.bizId(), STATUS_ACTIVE, now
        ), tokenProvider.getRefreshExpirationSeconds());
        sessionStore.bindUserSession(account.userId(), sessionId, refreshTokenId);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                now.plusSeconds(tokenProvider.getAccessExpirationSeconds()),
                now.plusSeconds(tokenProvider.getRefreshExpirationSeconds()),
                account.userId(),
                account.username(),
                account.role(),
                account.userType(),
                account.bizId(),
                sessionId,
                refreshTokenId
        );
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        JwtUserClaims claims = tokenProvider.parseRefreshToken(request.refreshToken());
        String refreshTokenId = tokenProvider.extractRefreshTokenId(request.refreshToken());
        RefreshSession refreshSession = sessionStore.getRefreshToken(refreshTokenId);
        if (refreshSession == null || !refreshSession.sessionId().equals(claims.sessionId())) {
            throw new IllegalArgumentException("刷新令牌已失效");
        }

        LoginSession loginSession = sessionStore.getSession(claims.sessionId());
        if (loginSession == null) {
            throw new IllegalArgumentException("登录会话已失效");
        }

        String accessToken = tokenProvider.generateAccessToken(
                claims.userId(), loginSession.username(), claims.role(), claims.userType(), claims.bizId(), claims.sessionId()
        );

        return new RefreshTokenResponse(
                accessToken,
                "Bearer",
                Instant.now().plusSeconds(tokenProvider.getAccessExpirationSeconds()),
                claims.sessionId()
        );
    }

    @Override
    public void logout(String accessToken) {
        JwtUserClaims claims = tokenProvider.parseAccessToken(accessToken);
        LoginSession session = sessionStore.getSession(claims.sessionId());
        if (session != null) {
            sessionStore.removeSession(session.sessionId());
            sessionStore.removeRefreshToken(session.refreshTokenId());
        }
        sessionStore.removeUserSessionBinding(claims.userId());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void register(com.neusoft.neu23.neuhospital.auth.dto.RegisterReq req) {
        // 1. 检查 username 是否已存在
        Long userCount = sysUserMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.neusoft.neu23.neuhospital.system.entity.SysUserEntity>().eq("username", req.getUsername()));
        if (userCount > 0) {
            throw new IllegalArgumentException("该用户名已被注册");
        }

        // 2. 检查手机号或身份证是否已在患者表中存在
        Long patientCount = patientMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.neusoft.neu23.neuhospital.patient.entity.PatientEntity>().eq("phone", req.getPhone()));
        if (patientCount > 0) {
            throw new IllegalArgumentException("该手机号已建档，请直接登录");
        }

        // 3. 创建患者档案 (Patient)
        String patientNo = "PAT-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        com.neusoft.neu23.neuhospital.patient.entity.PatientEntity patient = new com.neusoft.neu23.neuhospital.patient.entity.PatientEntity();
        patient.setPatientNo(patientNo);
        patient.setName(req.getRealName());
        patient.setGender(req.getGender() != null ? req.getGender() : "UNKNOWN");
        patient.setPhone(req.getPhone());
        patient.setIdCard(req.getIdCard());
        patient.setStatus("ENABLED");
        patient.setCreatedAt(java.time.LocalDateTime.now());
        patient.setUpdatedAt(java.time.LocalDateTime.now());
        patientMapper.insert(patient);

        // 4. 创建系统账号 (SysUser)
        com.neusoft.neu23.neuhospital.system.entity.SysUserEntity user = new com.neusoft.neu23.neuhospital.system.entity.SysUserEntity();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword())); // 使用 BCrypt 强哈希加密
        user.setUserType("PATIENT");
        user.setBizId(patient.getId()); // 关联业务ID
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setStatus("ENABLED");
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        sysUserMapper.insert(user);
    }

    @Override
    public com.neusoft.neu23.neuhospital.auth.vo.UserProfileResponse getCurrentUser(String accessToken) {
        JwtUserClaims claims = tokenProvider.parseAccessToken(accessToken);
        LoginSession session = sessionStore.getSession(claims.sessionId());
        if (session == null) {
            throw new IllegalArgumentException("登录会话已失效");
        }
        return new com.neusoft.neu23.neuhospital.auth.vo.UserProfileResponse(
                claims.userId(),
                session.username(),
                claims.role(),
                claims.userType(),
                claims.bizId()
        );
    }
}
