package com.neusoft.neu23.neuhospital.ai.application.agent;

public final class AiAgentSessionContext {

    private static final ThreadLocal<SessionScope> CONTEXT = new ThreadLocal<>();

    private AiAgentSessionContext() {
    }

    public static void bind(Long sessionId, Long patientId, Long registrationId) {
        CONTEXT.set(new SessionScope(sessionId, patientId, registrationId));
    }

    public static Long requirePatientId() {
        SessionScope scope = requireScope();
        if (scope.patientId() == null) {
            throw new IllegalStateException("当前AI会话缺少患者上下文");
        }
        return scope.patientId();
    }

    public static Long requireSessionId() {
        SessionScope scope = requireScope();
        if (scope.sessionId() == null) {
            throw new IllegalStateException("当前AI会话缺少会话上下文");
        }
        return scope.sessionId();
    }

    public static Long getRegistrationId() {
        SessionScope scope = CONTEXT.get();
        return scope == null ? null : scope.registrationId();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static SessionScope requireScope() {
        SessionScope scope = CONTEXT.get();
        if (scope == null) {
            throw new IllegalStateException("当前AI会话上下文不存在");
        }
        return scope;
    }

    private record SessionScope(Long sessionId, Long patientId, Long registrationId) {
    }
}
