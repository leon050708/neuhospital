# Agent Identity And Session Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 AI 会话中的患者身份映射错误和 session 越权漏洞，保留可安全迁移的历史会话，并补齐防串人测试。

**Architecture:** 以“服务端上下文绑定患者身份”为核心，统一从认证上下文解析当前患者业务 ID，禁止继续把 `sys_user.id` 当成 `patient.id` 使用。聊天入口改为按“会话 + 当前患者”双重校验，历史会话通过安全迁移服务修复归属；无法自动判定的旧数据保留并输出人工复核清单。

**Tech Stack:** Spring Boot 3.2, Spring Security, MyBatis-Plus, Spring AI, JUnit 5, Mockito

---

### Task 1: 收口当前患者身份解析

**Files:**
- Modify: `src/main/java/com/neusoft/neu23/neuhospital/auth/security/SecurityUtils.java`
- Modify: `src/main/java/com/neusoft/neu23/neuhospital/ai/controller/ChatController.java`
- Test: `src/test/java/com/neusoft/neu23/neuhospital/auth/security/SecurityUtilsTest.java`

- [ ] **Step 1: 写出当前患者身份解析的失败测试**

```java
package com.neusoft.neu23.neuhospital.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnBizIdForPatientUser() {
        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertEquals(35L, SecurityUtils.getCurrentPatientId());
    }

    @Test
    void shouldRejectNonPatientUserWhenResolvingCurrentPatientId() {
        CustomUserDetails principal = new CustomUserDetails(18L, "dr_li", "DOCTOR", "DOCTOR", 8L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentPatientId);
    }
}
```

- [ ] **Step 2: 运行失败测试确认当前行为不满足要求**

Run: `./mvnw -Dtest=SecurityUtilsTest test`
Expected: FAIL，`SecurityUtils` 中不存在 `getCurrentPatientId()` 或未校验 `userType=PATIENT`

- [ ] **Step 3: 在认证工具类中加入明确的患者身份解析方法**

```java
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
```

- [ ] **Step 4: 让聊天会话创建入口改用当前患者业务 ID**

```java
@PostMapping
public Result<AiChatSessionEntity> createSession(@RequestBody ChatSessionCreateReq req) {
    Long currentPatientId;
    try {
        currentPatientId = SecurityUtils.getCurrentPatientId();
    } catch (IllegalStateException ex) {
        return Result.error(401, ex.getMessage());
    }

    AiChatSessionEntity session = chatAgentService.createSession(
            currentPatientId,
            req.getRegistrationId(),
            req.getSessionType());
    return Result.success(session);
}
```

- [ ] **Step 5: 重新运行患者身份解析测试**

Run: `./mvnw -Dtest=SecurityUtilsTest test`
Expected: PASS

- [ ] **Step 6: 提交这一小步修复**

```bash
git add src/main/java/com/neusoft/neu23/neuhospital/auth/security/SecurityUtils.java src/main/java/com/neusoft/neu23/neuhospital/ai/controller/ChatController.java src/test/java/com/neusoft/neu23/neuhospital/auth/security/SecurityUtilsTest.java
git commit -m "fix: resolve current patient id from biz binding"
```

### Task 2: 收紧聊天 session 归属校验

**Files:**
- Modify: `src/main/java/com/neusoft/neu23/neuhospital/ai/controller/ChatController.java`
- Modify: `src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java`
- Test: `src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java`

- [ ] **Step 1: 先写 service 层的越权失败测试**

```java
package com.neusoft.neu23.neuhospital.ai.application.agent;

import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatMessageService;
import com.neusoft.neu23.neuhospital.ai.infrastructure.service.AiChatSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAgentServiceTest {

    @Test
    void shouldRejectAccessWhenSessionDoesNotBelongToCurrentPatient() {
        AiChatSessionService sessionService = mock(AiChatSessionService.class);
        AiChatMessageService messageService = mock(AiChatMessageService.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);

        when(builder.defaultFunctions("getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration"))
                .thenReturn(builder);
        when(builder.build()).thenReturn(client);

        ChatAgentService service = new ChatAgentService(builder, sessionService, messageService);

        assertThrows(IllegalArgumentException.class, () -> service.chat(101L, 35L, "我头疼"));
    }
}
```

- [ ] **Step 2: 运行测试确认当前接口只有 `chat(sessionId, userMessage)`**

Run: `./mvnw -Dtest=ChatAgentServiceTest test`
Expected: FAIL，`ChatAgentService` 还没有基于当前患者的归属校验入口

- [ ] **Step 3: 将聊天 service 改为显式接收当前患者 ID 并按归属加载会话**

```java
public String chat(Long sessionId, Long currentPatientId, String userMessage) {
    AiChatSessionEntity session = sessionService.getOne(new QueryWrapper<AiChatSessionEntity>()
            .eq("id", sessionId)
            .eq("patient_id", currentPatientId)
            .eq("status", "ENABLED")
            .eq("deleted", false)
            .last("LIMIT 1"));
    if (session == null) {
        throw new IllegalArgumentException("会话不存在或无权访问");
    }

    return doChat(session, userMessage);
}
```

- [ ] **Step 4: 让 controller 改成把当前患者 ID 传入 service，而不是自己只查 session 是否存在**

```java
@PostMapping("/{id}/messages")
public Result<String> sendMessage(@PathVariable("id") Long sessionId,
                                  @RequestBody ChatMessageReq req) {
    Long currentPatientId;
    try {
        currentPatientId = SecurityUtils.getCurrentPatientId();
    } catch (IllegalStateException ex) {
        return Result.error(401, ex.getMessage());
    }

    try {
        String response = chatAgentService.chat(sessionId, currentPatientId, req.getContent());
        return Result.success(response);
    } catch (IllegalArgumentException ex) {
        return Result.error(404, ex.getMessage());
    }
}
```

- [ ] **Step 5: 修正消息构造中的重复用户消息 bug，避免历史中同一轮用户发言被发两次**

```java
// 当前用户消息已在保存后写入 history，因此不再额外 add(new UserMessage(userMessage))
for (AiChatMessageEntity msg : history) {
    if ("USER".equals(msg.getMessageRole())) {
        messages.add(new UserMessage(msg.getMessageContent()));
    } else if ("ASSISTANT".equals(msg.getMessageRole())) {
        messages.add(new AssistantMessage(msg.getMessageContent()));
    }
}
```

- [ ] **Step 6: 重新运行聊天 service 测试**

Run: `./mvnw -Dtest=ChatAgentServiceTest test`
Expected: PASS

- [ ] **Step 7: 提交 session 归属修复**

```bash
git add src/main/java/com/neusoft/neu23/neuhospital/ai/controller/ChatController.java src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java
git commit -m "fix: enforce chat session ownership for patients"
```

### Task 3: 保留历史会话并安全迁移旧归属数据

**Files:**
- Create: `src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationService.java`
- Create: `src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationSummary.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationServiceTest.java`

- [ ] **Step 1: 先写迁移服务的失败测试，覆盖“只修明确错误的数据”**

```java
package com.neusoft.neu23.neuhospital.ai.application.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyChatSessionMigrationServiceTest {

    @Test
    void shouldOnlyMigrateSessionsWhosePatientIdMatchesPatientUserAccountIdButNotPatientTableId() {
        LegacyChatSessionMigrationSummary summary = new LegacyChatSessionMigrationSummary(1, 1, 0, 0);

        assertEquals(1, summary.scanned());
        assertEquals(1, summary.migrated());
    }
}
```

- [ ] **Step 2: 运行失败测试，确认迁移摘要类型尚不存在**

Run: `./mvnw -Dtest=LegacyChatSessionMigrationServiceTest test`
Expected: FAIL，迁移服务与摘要类型尚未创建

- [ ] **Step 3: 创建迁移摘要对象，明确记录扫描、修复、跳过、歧义数量**

```java
package com.neusoft.neu23.neuhospital.ai.application.agent;

public record LegacyChatSessionMigrationSummary(
        int scanned,
        int migrated,
        int skipped,
        int ambiguous
) {
}
```

- [ ] **Step 4: 创建安全迁移服务，只自动修复“明确可判定”的旧会话**

```java
public LegacyChatSessionMigrationSummary migrate() {
    List<AiChatSessionEntity> sessions = sessionService.list();
    int scanned = 0;
    int migrated = 0;
    int skipped = 0;
    int ambiguous = 0;

    for (AiChatSessionEntity session : sessions) {
        scanned++;

        PatientEntity patient = patientMapper.selectById(session.getPatientId());
        SysUserEntity patientUser = sysUserMapper.selectById(session.getPatientId());

        boolean missingPatient = patient == null;
        boolean patientAccountIdMatch = patientUser != null
                && "PATIENT".equals(patientUser.getUserType())
                && patientUser.getBizId() != null;

        if (missingPatient && patientAccountIdMatch) {
            session.setPatientId(patientUser.getBizId());
            session.setUpdatedAt(LocalDateTime.now());
            sessionService.updateById(session);
            migrated++;
            continue;
        }

        if (patient != null && patientAccountIdMatch && !session.getPatientId().equals(patientUser.getBizId())) {
            ambiguous++;
            continue;
        }

        skipped++;
    }

    return new LegacyChatSessionMigrationSummary(scanned, migrated, skipped, ambiguous);
}
```

- [ ] **Step 5: 加一个显式开关，避免上线即自动改库**

```yaml
app:
  ai:
    chat:
      migrate-legacy-session-patient-id-on-startup: false
```

- [ ] **Step 6: 用启动日志或 runner 触发可控迁移，并打印歧义会话数量**

```java
if (migrateOnStartup) {
    LegacyChatSessionMigrationSummary summary = migrationService.migrate();
    log.info("legacy chat session migration finished: {}", summary);
}
```

- [ ] **Step 7: 重新运行迁移服务测试**

Run: `./mvnw -Dtest=LegacyChatSessionMigrationServiceTest test`
Expected: PASS

- [ ] **Step 8: 提交安全迁移实现**

```bash
git add src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationService.java src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationSummary.java src/main/resources/application.yml src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/LegacyChatSessionMigrationServiceTest.java
git commit -m "feat: add safe migration for legacy chat session owners"
```

### Task 4: 补齐防串人回归测试并验证关键链路

**Files:**
- Create: `src/test/java/com/neusoft/neu23/neuhospital/ai/controller/ChatControllerTest.java`
- Modify: `src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java`

- [ ] **Step 1: 写 controller 层测试，验证患者 A 无法访问患者 B 的 session**

```java
package com.neusoft.neu23.neuhospital.ai.controller;

import com.neusoft.neu23.neuhospital.ai.application.agent.ChatAgentService;
import com.neusoft.neu23.neuhospital.auth.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ChatControllerTest {

    @Test
    void shouldReturn404WhenPatientTriesToAccessOthersSession() {
        ChatAgentService chatAgentService = mock(ChatAgentService.class);
        doThrow(new IllegalArgumentException("会话不存在或无权访问"))
                .when(chatAgentService).chat(anyLong(), anyLong(), anyString());

        CustomUserDetails principal = new CustomUserDetails(12L, "13800001111", "PATIENT", "PATIENT", 35L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        // 此处构造 controller 并断言返回 404
    }
}
```

- [ ] **Step 2: 运行 controller 测试确认会话越权路径已被覆盖**

Run: `./mvnw -Dtest=ChatControllerTest test`
Expected: FAIL 或编译失败，直到 controller 返回码和 service 调用签名都稳定下来

- [ ] **Step 3: 在聊天 service 测试里补“归属正确可继续聊天”的正向场景**

```java
@Test
void shouldAllowChatWhenSessionBelongsToCurrentPatient() {
    AiChatSessionEntity session = new AiChatSessionEntity();
    session.setId(101L);
    session.setPatientId(35L);
    session.setStatus("ENABLED");
    session.setDeleted(false);

    when(sessionService.getOne(any(QueryWrapper.class))).thenReturn(session);
    when(messageService.list(any(QueryWrapper.class))).thenReturn(List.of());
    when(messageService.count(any(QueryWrapper.class))).thenReturn(0L);

    // mock chatClient.prompt().messages(...).call().content() 返回固定回复
}
```

- [ ] **Step 4: 运行本轮所有 AI 身份与归属测试**

Run: `./mvnw -Dtest=SecurityUtilsTest,ChatAgentServiceTest,ChatControllerTest,LegacyChatSessionMigrationServiceTest test`
Expected: PASS

- [ ] **Step 5: 做一次人工验证清单，确认没有串人**

Run: `./mvnw test`
Expected: PASS

人工回归：
- 患者 A 创建会话后，`ai_chat_session.patient_id` 等于患者表主键，不等于 `sys_user.id`
- 患者 B 拿自己的 token 请求患者 A 的 `sessionId`，接口返回 404 或权限错误
- 历史会话迁移后，消息仍挂在原 `session_id` 下，只是 `patient_id` 归属修正
- 聊天一轮后，消息表中同一条用户消息只保存一次，不会重复进入模型上下文

- [ ] **Step 6: 提交测试与最终修复**

```bash
git add src/test/java/com/neusoft/neu23/neuhospital/ai/controller/ChatControllerTest.java src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java
git commit -m "test: add anti-cross-patient chat coverage"
```
