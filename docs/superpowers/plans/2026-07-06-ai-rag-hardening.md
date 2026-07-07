# AI RAG Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复当前 AI 对话与 RAG 链路中的状态不一致、误触发写操作、Embedding 漂移、摘要并发覆盖和检索性能隐患。

**Architecture:** 先做不改接口或少改接口的止血修复：同步知识文档与 chunk 状态、移除大模型自动调用写工具、按 embedding 模型过滤检索结果、串行化摘要落库。随后再把向量检索从“Java 全表扫描”升级为“Postgres 侧 TopK 检索”，保证规模扩大后仍可控。

**Tech Stack:** Spring Boot, Spring AI, MyBatis-Plus, PostgreSQL, Redisson, JUnit 5, Mockito

---

## File Map

- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminService.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchService.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfig.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java`
- Modify: `backend-service/src/main/resources/application.yml`
- Modify: `backend-service/src/main/resources/db/schema/phase3-ai-knowledge.sql`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminServiceTest.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchServiceTest.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfigTest.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java`
- Create: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/infrastructure/jdbc/KnowledgeVectorSearchRepository.java`
- Create: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeVectorSearchRepositoryTest.java`

### Task 1: 修复知识文档状态与检索状态断链

**Files:**
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminService.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminServiceTest.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchServiceTest.java`

- [ ] **Step 1: 先写两个失败测试，锁住“发布同步”和“下线同步”行为**

```java
@Test
void shouldSyncChunkStatusWhenPublishingDraftDocument() {
    FileService fileService = mock(FileService.class);
    KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
    KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
    KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
    MinioProperties properties = new MinioProperties();

    KnowledgeAdminService adminService = new KnowledgeAdminService(
            fileService, documentService, chunkService, ingestService, properties
    );

    KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
    document.setId(101L);
    document.setStatus("DRAFT");
    document.setParserStatus("EMBEDDED");
    document.setChunkCount(3);
    when(documentService.getById(101L)).thenReturn(document);

    adminService.publishDocument(101L, 9001L);

    verify(chunkService).update(
            any(),
            argThat(wrapper -> wrapper.getSqlSegment().contains("document_id"))
    );
}

@Test
void shouldSyncChunkStatusWhenOffliningPublishedDocument() {
    FileService fileService = mock(FileService.class);
    KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
    KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
    KnowledgeDocumentIngestService ingestService = mock(KnowledgeDocumentIngestService.class);
    MinioProperties properties = new MinioProperties();

    KnowledgeAdminService adminService = new KnowledgeAdminService(
            fileService, documentService, chunkService, ingestService, properties
    );

    KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
    document.setId(101L);
    document.setStatus("PUBLISHED");
    when(documentService.getById(101L)).thenReturn(document);

    adminService.offlineDocument(101L, 9001L);

    verify(chunkService).update(
            any(),
            argThat(wrapper -> wrapper.getSqlSegment().contains("document_id"))
    );
}
```

- [ ] **Step 2: 运行测试，确认当前实现确实没有同步 chunk 状态**

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeAdminServiceTest" test`

Expected: FAIL，报错点应是 `chunkService.update(...)` 从未被调用。

- [ ] **Step 3: 在发布/下线时同步更新 chunk，并阻止未完成索引的文档被发布**

```java
@Service
public class KnowledgeAdminService {

    private final FileService fileService;
    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final KnowledgeDocumentIngestService ingestService;
    private final MinioProperties minioProperties;

    public KnowledgeAdminService(FileService fileService,
                                 KnowledgeDocumentService documentService,
                                 KnowledgeChunkService chunkService,
                                 KnowledgeDocumentIngestService ingestService,
                                 MinioProperties minioProperties) {
        this.fileService = fileService;
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.ingestService = ingestService;
        this.minioProperties = minioProperties;
    }

    public void publishDocument(Long documentId, Long operatorId) {
        KnowledgeDocumentEntity document = requireDocument(documentId);
        if (!"EMBEDDED".equals(document.getParserStatus()) || document.getChunkCount() == null || document.getChunkCount() <= 0) {
            throw new BusinessException("知识文档尚未完成切片入库，不能发布");
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("PUBLISHED");
        update.setPublishedAt(now);
        update.setUpdatedAt(now);
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);

        KnowledgeChunkEntity chunkUpdate = new KnowledgeChunkEntity();
        chunkUpdate.setDocumentStatus("PUBLISHED");
        chunkUpdate.setUpdatedAt(now);
        chunkService.update(
                chunkUpdate,
                new QueryWrapper<KnowledgeChunkEntity>()
                        .eq("document_id", documentId)
                        .eq("deleted", false)
        );
    }

    public void offlineDocument(Long documentId, Long operatorId) {
        requireDocument(documentId);
        LocalDateTime now = LocalDateTime.now();

        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus("OFFLINE");
        update.setOfflineAt(now);
        update.setUpdatedAt(now);
        update.setUpdatedBy(operatorId);
        documentService.updateById(update);

        KnowledgeChunkEntity chunkUpdate = new KnowledgeChunkEntity();
        chunkUpdate.setDocumentStatus("OFFLINE");
        chunkUpdate.setUpdatedAt(now);
        chunkService.update(
                chunkUpdate,
                new QueryWrapper<KnowledgeChunkEntity>()
                        .eq("document_id", documentId)
                        .eq("deleted", false)
        );
    }

    private KnowledgeDocumentEntity requireDocument(Long documentId) {
        KnowledgeDocumentEntity document = documentService.getById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            throw new BusinessException("知识文档不存在");
        }
        return document;
    }
}
```

- [ ] **Step 4: 再补一个搜索层测试，确保 `document_status=OFFLINE` 的 chunk 不再被召回**

```java
@Test
void shouldIgnoreOfflineChunks() {
    KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
    EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

    KnowledgeSearchService searchService = new KnowledgeSearchService(
            chunkService, embeddingModel, "text-embedding-v3", 1
    );

    KnowledgeChunkEntity offline = new KnowledgeChunkEntity();
    offline.setId(9L);
    offline.setDocumentStatus("OFFLINE");
    offline.setEmbeddingModel("text-embedding-v3");
    offline.setEmbeddingVersion(1);
    offline.setEmbeddingText("[1.0,0.0]");
    offline.setContentText("已下线文档");

    when(chunkService.list(any())).thenReturn(List.of());
    when(embeddingModel.embed("挂号")).thenReturn(new float[]{1.0f, 0.0f});

    List<KnowledgeSearchHit> hits = searchService.search(new KnowledgeSearchRequest(
            "挂号", List.of("REGISTRATION_PROCESS"), null, null, 5, 0.1
    ));

    assertTrue(hits.isEmpty());
}
```

- [ ] **Step 5: 运行相关测试并提交**

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeAdminServiceTest,KnowledgeSearchServiceTest" test`

Expected: PASS

```bash
git add backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminService.java \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminServiceTest.java \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchServiceTest.java
git commit -m "fix: sync knowledge chunk status with document state"
```

### Task 2: 禁止大模型自动触发写操作工具

**Files:**
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfig.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfigTest.java`

- [ ] **Step 1: 先写失败测试，明确 Agent 默认只允许读工具**

```java
@Test
void shouldRegisterOnlyReadOnlyFunctionsForAgentChatClient() {
    ChatClient.Builder rootBuilder = mock(ChatClient.Builder.class);
    ChatClient.Builder agentBuilder = mock(ChatClient.Builder.class);
    ChatClient chatClient = mock(ChatClient.class);
    AiModelProperties properties = new AiModelProperties();
    properties.setAgentModel("qwen-plus");

    when(rootBuilder.clone()).thenReturn(agentBuilder);
    when(agentBuilder.defaultOptions(any())).thenReturn(agentBuilder);
    when(agentBuilder.defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule"))
            .thenReturn(agentBuilder);
    when(agentBuilder.build()).thenReturn(chatClient);

    new AiChatClientConfig().agentChatClient(rootBuilder, properties);

    verify(agentBuilder).defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule");
    verify(agentBuilder, never()).defaultFunctions(
            "getPatientInfo", "updatePatientMemory", "queryDepartment", "querySchedule", "bookRegistration"
    );
}
```

- [ ] **Step 2: 运行测试，确认当前配置确实还把写工具暴露给模型**

Run: `./mvnw -pl backend-service "-Dtest=AiChatClientConfigTest" test`

Expected: FAIL，断言显示当前仍注册了 `updatePatientMemory` 和 `bookRegistration`。

- [ ] **Step 3: 改 Agent ChatClient，只保留读工具；并在对话服务里给出“需要人工确认”的文案约束**

```java
@Configuration
public class AiChatClientConfig {

    @Bean("agentChatClient")
    public ChatClient agentChatClient(ChatClient.Builder rootBuilder, AiModelProperties properties) {
        return rootBuilder.clone()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAgentModel())
                        .build())
                .defaultFunctions("getPatientInfo", "queryDepartment", "querySchedule")
                .build();
    }
}
```

```java
private List<Message> buildPromptMessages(AiChatSessionEntity session,
                                          List<AiChatMessageEntity> unsummarizedHistory,
                                          PatientAgentRagContext ragContext) {
    List<Message> messages = new ArrayList<>();
    SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPromptResource);
    Message systemMessage = promptTemplate.createMessage(Map.of("patientId", session.getPatientId()));
    messages.add(systemMessage);
    if (session.getSummary() != null && !session.getSummary().trim().isEmpty()) {
        messages.add(new SystemMessage("历史前情摘要：" + session.getSummary()));
    }
    messages.add(new SystemMessage(ragContext.evidenceSystemMessage()));
    messages.add(new SystemMessage("""
            [工具安全约束]
            1. 你只能调用查询类工具，不能直接执行会改变业务状态的操作。
            2. 涉及挂号、建档修改、长期记忆写入时，只能告诉用户需要人工确认或走显式业务接口。
            3. 不要把“我建议下一步操作”伪装成“我已经替你执行成功”。
            """));
    for (AiChatMessageEntity msg : unsummarizedHistory) {
        if ("USER".equals(msg.getMessageRole())) {
            messages.add(new UserMessage(msg.getMessageContent()));
        } else if ("ASSISTANT".equals(msg.getMessageRole())) {
            messages.add(new AssistantMessage(msg.getMessageContent()));
        }
    }
    return messages;
}
```

- [ ] **Step 4: 补充回归验证，确认分析模型仍不挂工具，Agent 只挂 3 个读工具**

Run: `./mvnw -pl backend-service "-Dtest=AiChatClientConfigTest,ChatAgentServiceTest" test`

Expected: PASS

- [ ] **Step 5: 提交“先止血”的安全改动**

```bash
git add backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfig.java \
        backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/AiChatClientConfigTest.java
git commit -m "fix: remove write tools from llm auto-call path"
```

### Task 3: 处理 Embedding 模型漂移，避免旧向量静默失效

**Files:**
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchService.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminService.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchServiceTest.java`

- [ ] **Step 1: 先写失败测试，确认搜索会忽略模型不匹配的 chunk**

```java
@Test
void shouldIgnoreChunksFromDifferentEmbeddingModel() {
    KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
    EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    KnowledgeSearchService searchService = new KnowledgeSearchService(
            chunkService, embeddingModel, "text-embedding-v3", 1
    );

    KnowledgeChunkEntity sameModel = new KnowledgeChunkEntity();
    sameModel.setId(1L);
    sameModel.setContentText("挂号流程说明");
    sameModel.setEmbeddingText("[1.0,0.0]");
    sameModel.setEmbeddingModel("text-embedding-v3");
    sameModel.setEmbeddingVersion(1);

    KnowledgeChunkEntity differentModel = new KnowledgeChunkEntity();
    differentModel.setId(2L);
    differentModel.setContentText("旧模型数据");
    differentModel.setEmbeddingText("[1.0,0.0,0.0]");
    differentModel.setEmbeddingModel("text-embedding-ada-002");
    differentModel.setEmbeddingVersion(1);

    when(chunkService.list(any())).thenReturn(List.of(sameModel, differentModel));
    when(embeddingModel.embed("怎么挂号")).thenReturn(new float[]{1.0f, 0.0f});

    List<KnowledgeSearchHit> hits = searchService.search(new KnowledgeSearchRequest(
            "怎么挂号", List.of("REGISTRATION_PROCESS"), null, null, 5, 0.1
    ));

    assertEquals(1, hits.size());
    assertEquals(1L, hits.get(0).chunkId());
}
```

- [ ] **Step 2: 运行测试，确认当前搜索逻辑还没有利用 `embedding_model` 和 `embedding_version`**

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeSearchServiceTest" test`

Expected: FAIL，当前实现会把不同模型的数据一起参与比较，或者因为维度不等导致命中数异常。

- [ ] **Step 3: 在搜索层显式按当前模型/版本过滤，并给管理侧补一个“重建索引”入口**

```java
@Service
public class KnowledgeSearchService {

    private final KnowledgeChunkService chunkService;
    private final EmbeddingModel embeddingModel;
    private final String currentEmbeddingModel;
    private final int currentEmbeddingVersion;

    public KnowledgeSearchService(KnowledgeChunkService chunkService,
                                  EmbeddingModel embeddingModel,
                                  @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}") String currentEmbeddingModel,
                                  @Value("${app.ai.embedding-version:1}") int currentEmbeddingVersion) {
        this.chunkService = chunkService;
        this.embeddingModel = embeddingModel;
        this.currentEmbeddingModel = currentEmbeddingModel;
        this.currentEmbeddingVersion = currentEmbeddingVersion;
    }

    public List<KnowledgeSearchHit> search(KnowledgeSearchRequest request) {
        QueryWrapper<KnowledgeChunkEntity> wrapper = new QueryWrapper<KnowledgeChunkEntity>()
                .eq("deleted", false)
                .eq("document_status", "PUBLISHED")
                .eq("embedding_model", currentEmbeddingModel)
                .eq("embedding_version", currentEmbeddingVersion);
        // 其余过滤条件保持不变
        ...
    }
}
```

```java
public int reindexDocument(Long documentId, Long operatorId) {
    KnowledgeDocumentEntity document = requireDocument(documentId);
    document.setUpdatedBy(operatorId);
    documentService.updateById(document);
    return ingestService.ingestDocument(documentId);
}
```

- [ ] **Step 4: 把配置项补全，并验证“同模型命中、异模型忽略”**

```yaml
app:
  ai:
    embedding-version: ${OPENAI_EMBEDDING_VERSION:1}
```

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeSearchServiceTest,KnowledgeAdminServiceTest" test`

Expected: PASS

- [ ] **Step 5: 提交模型漂移防护**

```bash
git add backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchService.java \
        backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeAdminService.java \
        backend-service/src/main/resources/application.yml \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchServiceTest.java
git commit -m "fix: guard rag search by embedding model and version"
```

### Task 4: 防止摘要异步任务覆盖更新

**Files:**
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java`
- Modify: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java`

- [ ] **Step 1: 先写失败测试，明确旧摘要任务不能覆盖新序号**

```java
@Test
void shouldSkipSummaryWriteWhenTargetSequenceIsStale() {
    ChatClient agentChatClient = mock(ChatClient.class);
    ChatClient analysisChatClient = mock(ChatClient.class);
    AiChatSessionService sessionService = mock(AiChatSessionService.class);
    AiChatMessageService messageService = mock(AiChatMessageService.class);
    RegistrationService registrationService = mock(RegistrationService.class);
    RedissonClient redissonClient = mock(RedissonClient.class);
    PatientAgentRagOrchestrator ragOrchestrator = mock(PatientAgentRagOrchestrator.class);

    ChatAgentService service = new ChatAgentService(
            agentChatClient, analysisChatClient, sessionService, messageService,
            registrationService, redissonClient, ragOrchestrator
    );

    AiChatSessionEntity freshSession = new AiChatSessionEntity();
    freshSession.setId(11L);
    freshSession.setLastSummarizedSeq(20);
    when(sessionService.getById(11L)).thenReturn(freshSession);

    service.completeSummaryUpdate(11L, 18, "旧摘要");

    verify(sessionService, never()).updateById(argThat(entity ->
            "旧摘要".equals(entity.getSummary())
    ));
}
```

- [ ] **Step 2: 运行测试，确认当前实现没有“新旧序号比较”这层保护**

Run: `./mvnw -pl backend-service "-Dtest=ChatAgentServiceTest" test`

Expected: FAIL，当前没有 `completeSummaryUpdate(...)` 这种可测入口，也没有 stale guard。

- [ ] **Step 3: 抽出一个可测试的摘要落库方法，异步线程里先加分布式锁，再基于最新会话序号更新**

```java
void completeSummaryUpdate(Long sessionId, int targetSequence, String newSummary) {
    AiChatSessionEntity latestSession = sessionService.getById(sessionId);
    if (latestSession == null) {
        return;
    }
    int currentSeq = latestSession.getLastSummarizedSeq() == null ? 0 : latestSession.getLastSummarizedSeq();
    if (targetSequence <= currentSeq) {
        return;
    }

    AiChatSessionEntity update = new AiChatSessionEntity();
    update.setId(sessionId);
    update.setSummary(newSummary);
    update.setLastSummarizedSeq(targetSequence);
    update.setUpdatedAt(LocalDateTime.now());
    sessionService.updateById(update);
}

private void triggerAsyncSummarization(AiChatSessionEntity session,
                                       List<AiChatMessageEntity> unsummarizedHistory,
                                       AiChatMessageEntity lastAssistantMsg) {
    CompletableFuture.runAsync(() -> {
        RLock summaryLock = redissonClient.getLock("medical:ai:summary:lock:" + session.getId());
        boolean locked = false;
        try {
            locked = summaryLock.tryLock(1, 30, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            String newSummary = ...;
            completeSummaryUpdate(session.getId(), lastAssistantMsg.getSequenceNo(), newSummary);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && summaryLock.isHeldByCurrentThread()) {
                summaryLock.unlock();
            }
        }
    });
}
```

- [ ] **Step 4: 运行单测，确认旧任务不会覆盖新摘要**

Run: `./mvnw -pl backend-service "-Dtest=ChatAgentServiceTest" test`

Expected: PASS

- [ ] **Step 5: 提交摘要并发修复**

```bash
git add backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentService.java \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/agent/ChatAgentServiceTest.java
git commit -m "fix: prevent stale async summaries from overwriting session state"
```

### Task 5: 把检索从 Java 全表扫描升级为数据库侧 TopK

**Files:**
- Create: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/infrastructure/jdbc/KnowledgeVectorSearchRepository.java`
- Modify: `backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchService.java`
- Modify: `backend-service/src/main/resources/db/schema/phase3-ai-knowledge.sql`
- Create: `backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeVectorSearchRepositoryTest.java`

- [ ] **Step 1: 先写仓储层失败测试，锁定“数据库只返回 TopK”的行为**

```java
@Test
void shouldQueryTopKInsideDatabase() {
    NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    KnowledgeVectorSearchRepository repository = new KnowledgeVectorSearchRepository(jdbcTemplate);

    when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
            .thenReturn(List.of(
                    new KnowledgeSearchHit(1L, 10L, "挂号流程", "REGISTRATION_PROCESS", null, "挂号", 0.91),
                    new KnowledgeSearchHit(2L, 10L, "初诊材料", "VISIT_PREPARATION", null, "初诊", 0.82)
            ));

    List<KnowledgeSearchHit> hits = repository.search(
            "[1.0,0.0]",
            List.of("REGISTRATION_PROCESS", "VISIT_PREPARATION"),
            null,
            null,
            "text-embedding-v3",
            1,
            4,
            0.6
    );

    assertEquals(2, hits.size());
    verify(jdbcTemplate).query(contains("LIMIT :topK"), any(MapSqlParameterSource.class), any(RowMapper.class));
}
```

- [ ] **Step 2: 运行测试，确认当前仓储类不存在，说明还没有数据库侧向量检索**

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeVectorSearchRepositoryTest" test`

Expected: FAIL，编译期提示 `KnowledgeVectorSearchRepository` 不存在。

- [ ] **Step 3: 引入 pgvector 列和 JDBC 检索仓储，把排序/过滤下推到数据库**

```sql
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS embedding_vector vector(1024);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_vector
    ON knowledge_chunk USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 100);
```

```java
@Repository
public class KnowledgeVectorSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KnowledgeVectorSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeSearchHit> search(String vectorLiteral,
                                           List<String> knowledgeTypes,
                                           Long departmentId,
                                           String tagKeyword,
                                           String embeddingModel,
                                           int embeddingVersion,
                                           int topK,
                                           double minScore) {
        String sql = """
                SELECT id, document_id, content_text, knowledge_type, department_id, tags,
                       1 - (embedding_vector <=> CAST(:queryVector AS vector)) AS score
                FROM knowledge_chunk
                WHERE deleted = false
                  AND document_status = 'PUBLISHED'
                  AND embedding_model = :embeddingModel
                  AND embedding_version = :embeddingVersion
                  AND (:departmentId IS NULL OR department_id = :departmentId)
                  AND (:tagKeyword IS NULL OR tags LIKE CONCAT('%%', :tagKeyword, '%%'))
                  AND (COALESCE(:knowledgeTypesEmpty, true) OR knowledge_type IN (:knowledgeTypes))
                  AND 1 - (embedding_vector <=> CAST(:queryVector AS vector)) >= :minScore
                ORDER BY embedding_vector <=> CAST(:queryVector AS vector)
                LIMIT :topK
                """;
        ...
    }
}
```

```java
public List<KnowledgeSearchHit> search(KnowledgeSearchRequest request) {
    float[] queryEmbedding = embeddingModel.embed(request.query());
    String vectorLiteral = toVectorLiteral(queryEmbedding);
    return vectorSearchRepository.search(
            vectorLiteral,
            request.knowledgeTypes(),
            request.departmentId(),
            request.tagKeyword(),
            currentEmbeddingModel,
            currentEmbeddingVersion,
            request.topK() > 0 ? request.topK() : 5,
            request.minScore()
    );
}
```

- [ ] **Step 4: 运行仓储层与服务层测试，确认不再依赖 Java 侧全量遍历**

Run: `./mvnw -pl backend-service "-Dtest=KnowledgeVectorSearchRepositoryTest,KnowledgeSearchServiceTest" test`

Expected: PASS

- [ ] **Step 5: 提交性能升级**

```bash
git add backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/infrastructure/jdbc/KnowledgeVectorSearchRepository.java \
        backend-service/src/main/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeSearchService.java \
        backend-service/src/main/resources/db/schema/phase3-ai-knowledge.sql \
        backend-service/src/test/java/com/neusoft/neu23/neuhospital/ai/application/rag/KnowledgeVectorSearchRepositoryTest.java
git commit -m "perf: move rag vector ranking into postgres"
```

## Recommended Execution Order

1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5

## Notes

- Task 1 到 Task 4 都属于线上风险修复，优先级高。
- Task 5 是架构升级，不建议和前四个任务混在一个提交里做。
- 如果你们近期不会切换 embedding 模型，Task 3 里“重建索引入口”可以先只做单文档重建，不急着做批量任务。
- 如果产品现在不允许 AI 直接代用户挂号，Task 2 做完就够了；后面如需恢复“AI 辅助挂号”，应走“显式确认接口”，不要再把写操作直接暴露给模型。
