package com.neusoft.neu23.neuhospital.auth.security;

/**
 * Abstraction over session persistence so service logic is testable without Redis.
 */
public interface LoginSessionStore {

    void saveSession(LoginSession session, long ttlSeconds);

    LoginSession getSession(String sessionId);

    void removeSession(String sessionId);

    void saveRefreshToken(RefreshSession session, long ttlSeconds);

    RefreshSession getRefreshToken(String refreshTokenId);

    void removeRefreshToken(String refreshTokenId);

    void bindUserSession(Long userId, String sessionId, String refreshTokenId);

    UserSessionBinding getUserSessionBinding(Long userId);

    void removeUserSessionBinding(Long userId);
}
