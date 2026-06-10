# Java 后端推荐架构说明

> 用途：用于组内沟通智慧云脑诊疗 Agent 平台的 Java 后端推荐目录结构、分层原则与后续扩展方式。

---

## 1. 总体设计思路

本项目建议采用：

```text
模块化单体 Java 后端 + 独立 Python 影像服务
```

其中：

- Java 后端负责医院核心业务、权限认证、数据库事务、Agent 编排、RAG、Tool Calling、Kafka 消息和文件管理
- Python 服务只负责 CT 影像预处理、模型推理和 Grad-CAM 热力图生成
- PostgreSQL、Redis、Kafka、MinIO 作为统一中间件基础设施

Java 后端整体拆分原则如下：

```text
1. 普通医院业务按业务域拆包
2. 登录认证单独抽成 auth 模块
3. AI 相关能力单独抽成 ai 能力中心
4. 普通业务模块内部继续使用 controller / service / mapper / entity / dto / vo 常规分层
5. 不按“患者端”和“医生端”拆成两套后端，而是通过同一套业务域 + 权限 + 接口角色控制来支持多端
```

---

## 2. 推荐主骨架

```text
smart-medical-backend/
├── common/
├── auth/
│   ├── controller/
│   ├── service/
│   ├── security/
│   ├── dto/
│   └── vo/
├── system/
├── patient/
├── doctor/
├── registration/
├── outpatient/
├── inspection/
├── pharmacy/
├── payment/
├── file/
├── image/
└── ai/
    ├── controller/
    ├── application/
    │   ├── agent/
    │   ├── tool/
    │   ├── rag/
    │   ├── memory/
    │   └── log/
    ├── domain/
    ├── infrastructure/
    └── config/
```

---

## 3. 为什么不直接按“医生端 / 患者端”拆包

医生端和患者端只是同一套医院业务的不同使用视角，而不是两套独立业务域。

例如：

- `patient` 模块既会被患者本人查看，也会被医生接诊时使用
- `registration` 模块既支持患者挂号，也支持医生查看待诊队列
- `outpatient` 模块既支持医生填写病历，也支持患者查看就诊结果
- `image` 模块既支持患者上传 CT，也支持医生查看分析结果

因此更合理的方式是：

```text
按业务域拆模块
+ 通过 JWT + 角色权限区分患者、医生、管理员
+ 通过接口设计区分不同端的调用方式
```

这样可以避免：

- 同一业务在患者端和医生端重复实现
- 目录重复、代码重复
- 领域边界混乱

---

## 4. 普通业务模块怎么放 controller / service

普通业务模块仍然保留传统分层，只是“每个业务模块自己带一套小分层”，而不是全项目共用一个大 `controller/`、一个大 `service/`。

以 `patient` 模块为例：

```text
patient/
├── controller/
├── service/
├── mapper/
├── entity/
├── dto/
└── vo/
```

职责如下：

- `controller/`：接口入口，接收请求并返回响应
- `service/`：业务逻辑
- `mapper/`：数据库访问
- `entity/`：表实体
- `dto/`：接口入参对象
- `vo/`：接口出参对象

这种方式比“全局一个 controller 包、一个 service 包”更适合医疗系统这类复杂业务项目。

---

## 5. 各主模块职责说明

### 5.1 `common/`

放全项目公共能力，不属于某个具体业务域。

典型内容：

- 统一返回体
- 全局异常
- 全局异常处理器
- 常量
- 枚举
- 工具类
- 通用配置

---

### 5.2 `auth/`

放登录认证和鉴权逻辑。

建议使用：

```text
JWT + Spring Security
```

子目录职责：

- `controller/`：登录、登出、刷新 token、获取当前用户
- `service/`：账号校验、token 签发与刷新
- `security/`：JWT 生成与校验、过滤器、Spring Security 配置
- `dto/`：登录请求等入参对象
- `vo/`：登录成功返回对象

说明：

```text
当前阶段不必引入 Gateway，也不必为了 JWT 先做微服务。
JWT 完全可以在单体 Java 后端内完成签发与校验。
```

---

### 5.3 `system/`

放系统管理能力。

典型内容：

- 用户管理
- 角色管理
- 权限管理
- 菜单管理
- 字典管理
- 操作日志
- 系统配置

`auth` 解决“你是谁、能不能进”，`system` 解决“平台里有哪些账号、角色和权限”。

---

### 5.4 `patient/`

放患者领域相关能力。

典型内容：

- 患者基本信息
- 过敏史
- 既往史
- 患者档案
- 患者历史资料

说明：

```text
这是患者领域，不是“患者端页面接口专属模块”。
医生接诊时也会使用这里的能力。
```

---

### 5.5 `doctor/`

放医生领域相关能力。

典型内容：

- 医生档案
- 科室归属
- 职称
- 医生排班关联
- 医生接诊基础信息

---

### 5.6 `registration/`

放挂号领域能力。

典型内容：

- 挂号
- 退号
- 号源控制
- 挂号记录
- 接诊队列

说明：

```text
患者端会用它挂号和退号，医生端会用它查看待诊列表，因此它是共享业务域。
```

---

### 5.7 `outpatient/`

放门诊接诊与病历领域。

典型内容：

- 门诊接诊
- 病历首页
- 主诉
- 现病史
- 初步诊断
- 门诊确诊

说明：

```text
该模块是后续 AI 病历草稿、AI 辅助诊断的重要依赖域。
```

---

### 5.8 `inspection/`

放检查、检验、处置相关能力。

典型内容：

- 检查申请
- 检验申请
- 处置申请
- 检查登记
- 结果录入
- 检查报告与附件

---

### 5.9 `pharmacy/`

放药房与药品相关能力。

典型内容：

- 药品信息
- 药品库存
- 发药
- 退药
- 库存预警

后续如果要做处方安全提醒 Agent，也会依赖该模块提供药品信息。

---

### 5.10 `payment/`

放收费和订单相关能力。

典型内容：

- 待缴费项目
- 缴费订单
- 缴费明细
- 收费
- 退费
- 日结统计

说明：

```text
该模块属于强事务场景，后续实现时要特别注意事务一致性。
```

---

### 5.11 `file/`

放统一文件服务能力。

典型内容：

- 文件上传
- 文件下载
- 文件元数据
- MinIO 对接
- 临时访问链接
- 文件格式校验

说明：

```text
CT 文件、检查报告、知识库文档、热力图等都属于文件能力，不应分散到各业务模块各自重复实现。
```

---

### 5.12 `image/`

放 Java 侧影像业务调度能力。

典型内容：

- CT 文件记录
- 影像分析任务
- 影像分析结果
- Kafka 发送影像任务
- Kafka 接收影像结果
- 调用 Python 影像服务
- 热力图地址管理

说明：

```text
image 模块负责影像业务调度，不负责模型训练和推理本身。
真正的算法推理放在独立 Python 服务中。
```

---

## 6. `ai/` 为什么不能只做成普通业务包

如果后续要做：

- Spring AI Agent
- Tool Calling
- RAG
- Chat Memory
- pgvector
- Python 影像解释
- 未来 MCP

那么如果 `ai/` 只是简单平铺：

```text
ai/
├── chat/
├── triage/
├── record/
├── diagnosis/
├── rag/
├── tools/
└── log/
```

后续很容易越来越乱，因为：

- prompt 放哪不清楚
- tool 和业务 service 容易混
- RAG 和 Agent 编排容易耦合
- pgvector、Redis、Kafka、Python、MCP 等技术接入层没有稳定位置

因此更推荐把 `ai/` 做成能力中心。

---

## 7. `ai/` 模块内部职责说明

### 7.1 `ai/controller/`

放 AI 相关接口入口。

比如：

- 智能问诊
- 智能导诊
- AI 病历草稿生成
- AI 辅助诊断
- 影像结果解释

---

### 7.2 `ai/application/`

放 AI 编排层，相当于 AI 子系统里的“应用服务层”。

它负责：

- 组装上下文
- 决定是否调用 RAG
- 决定是否调用工具
- 调用 Spring AI
- 保存消息和日志
- 返回最终结构化结果

#### `ai/application/agent/`

放各类 Agent 编排服务。

例如：

- ChatAgentService
- TriageAgentService
- RecordDraftAgentService
- DiagnosisAgentService
- ImageExplainAgentService

#### `ai/application/tool/`

放 Tool Calling 适配层。

注意：

```text
Tool 不是业务本体，只是 Agent 可调用的工具壳。
真正业务逻辑仍然在 patient / outpatient / image 等业务模块中。
```

#### `ai/application/rag/`

放知识库检索增强流程。

包括：

- 文档切分
- 向量入库
- 向量检索
- 引用来源组装

#### `ai/application/memory/`

放会话记忆和上下文管理。

包括：

- 会话创建
- 会话与患者绑定
- 多轮消息读取
- Redis / JDBC memory 协调

#### `ai/application/log/`

放 AI 调用日志和审计日志逻辑。

医疗系统中 AI 输出必须保留审计痕迹，因此这层很重要。

---

### 7.3 `ai/domain/`

放 AI 领域对象。

例如：

- 问诊结果对象
- 导诊结果对象
- 风险等级枚举
- 病历草稿对象
- 辅助诊断建议对象

这层用于定义 AI 输出的业务语义，避免后续大量使用无类型 Map。

---

### 7.4 `ai/infrastructure/`

放 AI 子系统依赖的外部技术接入层。

建议继续细分：

```text
ai/infrastructure/
├── model/          # ChatModel / EmbeddingModel
├── prompt/         # Prompt 模板加载与管理
├── vectorstore/    # pgvector 接入
├── redis/          # Redis memory / cache
├── jdbc/           # JDBC chat memory / AI 持久化
├── kafka/          # AI 日志 / 异步消息
├── minio/          # 文档 / 热力图 / 知识文件
├── python/         # Python 影像服务 client
└── mcp/            # 未来 MCP 接入
```

这样后面：

- 更换模型
- 调整向量检索实现
- 引入 MCP
- 扩展 Prompt 机制

时不会把业务编排层搞乱。

---

### 7.5 `ai/config/`

放 AI 相关配置类。

例如：

- Spring AI 配置
- ChatModel 配置
- EmbeddingModel 配置
- Tool 注册配置
- Prompt 配置
- RAG 配置
- Memory 配置

这些配置明显只服务于 AI 子系统，因此不建议放进 `common`。

---

## 8. 关于 JWT、Gateway、Nacos、微服务的建议

当前阶段建议采用：

```text
模块化单体 Java 后端 + JWT + 独立 Python 服务
```

当前不建议一开始就引入：

- API Gateway
- Nacos
- 大规模微服务拆分

原因：

- 当前项目仍处于业务闭环优先阶段
- 过早上 Gateway、Nacos、微服务会显著增加系统复杂度
- JWT 完全可以在当前 Java 后端内部完成签发与校验

推荐演进顺序：

```text
第一阶段：模块化单体 + JWT + 独立 Python 服务
第二阶段：中间件稳定后再补强 AI、RAG、影像协作
第三阶段：若后期团队规模和部署复杂度明显上升，再考虑 Gateway / Nacos / 微服务拆分
```

---

## 9. 最终结论

本项目最合适的后端组织方式不是：

- 纯按 controller / service 全局大包拆分
- 也不是按患者端 / 医生端拆成两套重复后端

而是：

```text
普通医院业务按业务域拆包
+ 普通业务模块内部继续使用 controller / service / mapper / entity / dto / vo 常规分层
+ AI 单独抽成能力中心
+ JWT 单独在 auth 模块中实现
+ Python 影像服务作为独立后端协作服务存在
```

这样既能兼顾：

- 患者端
- 医生端
- 管理端
- Agent 与 Tool Calling
- RAG 与 pgvector
- 影像分析服务协作
- 未来 MCP 与潜在微服务演进

又能避免后期目录结构和职责边界失控。
