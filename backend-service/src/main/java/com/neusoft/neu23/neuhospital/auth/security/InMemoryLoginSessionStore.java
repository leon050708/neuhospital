package com.neusoft.neu23.neuhospital.auth.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory session store used by tests.
 */
public class InMemoryLoginSessionStore implements LoginSessionStore {

    private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, RefreshSession> refreshSessions = new ConcurrentHashMap<>();
    private final Map<Long, UserSessionBinding> bindings = new ConcurrentHashMap<>();

    @Override
    public void saveSession(LoginSession session, long ttlSeconds) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public LoginSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public void saveRefreshToken(RefreshSession session, long ttlSeconds) {
        refreshSessions.put(session.refreshTokenId(), session);
    }

    @Override
    public RefreshSession getRefreshToken(String refreshTokenId) {
        return refreshSessions.get(refreshTokenId);
    }

    @Override
    public void removeRefreshToken(String refreshTokenId) {
        refreshSessions.remove(refreshTokenId);
    }

    @Override
    public void bindUserSession(Long userId, String sessionId, String refreshTokenId) {
        bindings.put(userId, new UserSessionBinding(userId, sessionId, refreshTokenId));
    }

    @Override
    public UserSessionBinding getUserSessionBinding(Long userId) {
        return bindings.get(userId);
    }

    @Override
    public void removeUserSessionBinding(Long userId) {
        bindings.remove(userId);
    }
}
