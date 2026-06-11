package com.neusoft.neu23.neuhospital.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed login-session store used at runtime.
 */
@Component
public class RedisLoginSessionStore implements LoginSessionStore {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String REFRESH_KEY_PREFIX = "login:refresh:";
    private static final String USER_BINDING_KEY_PREFIX = "login:user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisLoginSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveSession(LoginSession session, long ttlSeconds) {
        writeValue(SESSION_KEY_PREFIX + session.sessionId(), session, ttlSeconds);
    }

    @Override
    public LoginSession getSession(String sessionId) {
        return readValue(SESSION_KEY_PREFIX + sessionId, LoginSession.class);
    }

    @Override
    public void removeSession(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }

    @Override
    public void saveRefreshToken(RefreshSession session, long ttlSeconds) {
        writeValue(REFRESH_KEY_PREFIX + session.refreshTokenId(), session, ttlSeconds);
    }

    @Override
    public RefreshSession getRefreshToken(String refreshTokenId) {
        return readValue(REFRESH_KEY_PREFIX + refreshTokenId, RefreshSession.class);
    }

    @Override
    public void removeRefreshToken(String refreshTokenId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + refreshTokenId);
    }

    @Override
    public void bindUserSession(Long userId, String sessionId, String refreshTokenId) {
        writeValue(USER_BINDING_KEY_PREFIX + userId, new UserSessionBinding(userId, sessionId, refreshTokenId), null);
    }

    @Override
    public UserSessionBinding getUserSessionBinding(Long userId) {
        return readValue(USER_BINDING_KEY_PREFIX + userId, UserSessionBinding.class);
    }

    @Override
    public void removeUserSessionBinding(Long userId) {
        redisTemplate.delete(USER_BINDING_KEY_PREFIX + userId);
    }

    private void writeValue(String key, Object value, Long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttlSeconds == null) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize auth session data", ex);
        }
    }

    private <T> T readValue(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize auth session data", ex);
        }
    }
}
