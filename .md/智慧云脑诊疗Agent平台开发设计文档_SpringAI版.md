# 智慧云脑诊疗 Agent 平台开发设计文档（Spring AI 版）

> 版本：V2.0  
> 架构方向：Java Spring Boot + Spring AI Agent + Python CT 影像算法服务  
> 数据库：PostgreSQL  
> 缓存与锁：Redis  
> 消息队列：Kafka  
> 文件存储：MinIO  
> 影像模型：离线训练 + 在线推理  

---

## 1. 项目概述

### 1.1 项目名称

**基于 Spring AI 与 Python 影像服务的智慧云脑诊疗 Agent 平台**

简称：**智慧云脑诊疗 Agent 平台**

---

### 1.2 项目背景

随着互联网医院、人工智能、大语言模型和医学影像识别技术的发展，传统门诊系统已经不再局限于挂号、接诊、缴费、开药等基础业务流程，而是逐渐向“业务流程 + 智能问诊 + 智能导诊 + 辅助诊断 + 医学影像分析”的综合平台演进。

原始项目文档中已经包含较完整的传统门诊业务流程，例如窗口挂号、医生接诊、病历首页、检查申请、患者收费、检查结果录入、门诊确诊、开设处方和药房发药等功能。本项目在这些传统 HIS 门诊流程基础上，引入 Spring AI 大模型 Agent、医学知识库 RAG、CT 影像识别和 Grad-CAM 病灶热力图能力，构建面向云医院场景的智慧诊疗辅助系统。

本项目采用 Java 作为业务主后端，负责核心业务、权限认证、数据库事务、高并发控制和 Agent 编排；Python 作为独立影像算法微服务，负责 CT 医学影像预处理、脑出血模型推理和热力图生成。系统通过 Kafka、Redis、MinIO 和 PostgreSQL 完成异步任务、缓存、文件存储和数据持久化。

---

### 1.3 项目目标

本项目目标是开发一套面向云医院场景的 AI 诊疗辅助平台，重点实现以下能力：

1. 完成基础门诊业务流程，包括患者管理、医生管理、科室管理、排班管理、挂号、病历、检查申请、处方、缴费和药房发药等。
2. 基于 Spring AI 实现智能医学问诊 Agent，通过多轮对话理解患者主诉，并结合患者历史病历和过敏史提供安全的就医建议。
3. 基于 Spring AI 实现智能导诊 Agent，根据患者症状、风险表现和科室规则推荐合适科室。
4. 基于 Spring AI 实现医学知识库 RAG，使 Agent 能够结合医学指南、科室分诊规则、药品禁忌和检查项目说明进行回答。
5. 基于 Python FastAPI + PyTorch 实现脑出血 CT 影像识别服务，支持 DICOM、NIfTI 和 DICOM 压缩包上传分析。
6. 使用 CQ500 数据集进行离线模型训练，训练完成后将 `.pth` 权重接入 Python 影像推理服务。
7. 使用 Grad-CAM 生成疑似脑出血病灶热力图，辅助医生理解模型关注区域。
8. 使用 PostgreSQL 存储核心业务数据，Redis 处理缓存、会话和分布式锁，Kafka 处理异步影像分析任务，MinIO 存储医学影像和热力图。
9. 明确 AI 安全边界：AI 仅作为辅助建议，最终诊断和处方必须由医生确认。

---

## 2. 总体架构设计

### 2.1 架构定位

本项目采用：

```text
Vue 前端 + Java Spring Boot 业务后端 + Spring AI Agent + Python 影像算法服务
```

总体职责划分如下：

```text
Java Spring Boot：负责医院业务流程、权限认证、数据库事务、Redis 锁、Kafka 消息、Agent 编排
Spring AI：负责智能问诊、智能导诊、RAG 检索、工具调用、病历草稿生成、影像结果解释
Python FastAPI：负责 CT 影像预处理、模型加载、脑出血推理、Grad-CAM 热力图生成
PostgreSQL：负责核心业务数据和 AI 结果数据持久化
Redis：负责缓存、登录会话、问诊上下文、分布式锁和限流
Kafka：负责异步影像分析任务和 AI 日志异步处理
MinIO：负责 DICOM、NIfTI、热力图、检查报告和知识库文档等大文件存储
```

---

### 2.2 总体架构图

```text
┌──────────────────────────────────────┐
│              前端 Vue 3               │
│ 患者端 / 医生端 / 管理端 / 影像展示端   │
└───────────────────┬──────────────────┘
                    │ HTTP / SSE / WebSocket
                    ▼
┌──────────────────────────────────────┐
│        Java Spring Boot 后端           │
│ 业务模块 / 权限认证 / AI Agent / 网关调用 │
└───────────────┬───────────┬──────────┘
                │           │
                │           │
        ┌───────▼───────┐   │
        │  Spring AI     │   │
        │ Agent / RAG    │   │
        │ Tool Calling   │   │
        └───────┬───────┘   │
                │           │
                │           │
 ┌──────────────▼───┐   ┌───▼──────────────┐
 │   PostgreSQL      │   │ Redis / Kafka     │
 │ 业务数据 / AI记录  │   │ 缓存 / 锁 / 消息    │
 └──────────────────┘   └───┬──────────────┘
                             │ Kafka / HTTP
                             ▼
┌──────────────────────────────────────┐
│        Python FastAPI 影像服务         │
│ SimpleITK / PyTorch / OpenCV / Grad-CAM │
└───────────────────┬──────────────────┘
                    │
                    ▼
┌──────────────────────────────────────┐
│               MinIO                   │
│ DICOM / NIfTI / 热力图 / 检查报告 / 文档 │
└──────────────────────────────────────┘
```

---

## 3. 技术栈规划

### 3.1 前端技术栈

```text
Vue 3
TypeScript
Element Plus
Pinia
Vue Router
Axios
Vite
ECharts
SSE / WebSocket
```

| 技术 | 用途 |
|---|---|
| Vue 3 | 构建患者端、医生端和管理端页面 |
| TypeScript | 提高代码可维护性和类型安全 |
| Element Plus | 后台管理系统 UI 组件 |
| Pinia | 前端全局状态管理 |
| Vue Router | 页面路由管理 |
| Axios | 调用 Java 后端接口 |
| ECharts | 统计图表、影像分析结果可视化 |
| SSE / WebSocket | AI 问诊流式输出 |

---

### 3.2 Java 后端技术栈

```text
Java 17
Spring Boot 3.x
Spring MVC
Spring AI
Spring Security / Sa-Token
MyBatis-Plus
PostgreSQL
Redis
Kafka
MinIO SDK
OpenFeign / WebClient
Knife4j / Swagger
Maven
Docker
```

| 技术 | 用途 |
|---|---|
| Java 17 | 后端主开发语言 |
| Spring Boot 3.x | 构建后端服务 |
| Spring MVC | RESTful API 开发 |
| Spring AI | 大模型调用、RAG、工具调用、Agent 编排 |
| Sa-Token / Spring Security | 登录认证和权限控制 |
| MyBatis-Plus | 数据库访问 |
| PostgreSQL | 业务数据和 AI 数据存储 |
| Redis | 缓存、会话、分布式锁、限流 |
| Kafka | 异步影像分析任务和日志消息 |
| MinIO SDK | 文件上传、下载和临时访问链接生成 |
| WebClient / OpenFeign | 调用 Python 影像服务 |
| Knife4j / Swagger | 接口文档生成 |

---

### 3.3 Python 影像服务技术栈

```text
Python 3.10+
FastAPI
Uvicorn
Pydantic
PyTorch
SimpleITK
OpenCV
NumPy
pydicom
scikit-learn
Kafka Client
MinIO Python SDK
```

| 技术 | 用途 |
|---|---|
| FastAPI | 对 Java 暴露影像分析接口 |
| Uvicorn | Python Web 服务启动 |
| Pydantic | 请求和响应参数校验 |
| PyTorch | ResNet-50 模型训练和推理 |
| SimpleITK | DICOM/NIfTI 医学影像读取和处理 |
| OpenCV | 图像处理、热力图叠加 |
| NumPy | 图像矩阵计算 |
| pydicom | DICOM 元数据读取 |
| scikit-learn | 训练评估指标计算 |
| Kafka Client | 消费 Java 发送的影像分析任务 |
| MinIO SDK | 下载原始 CT 文件、上传热力图 |

---

### 3.4 数据库与中间件

| 中间件 | 主要用途 |
|---|---|
| PostgreSQL | 存储患者、医生、挂号、病历、处方、检查、AI 问诊、影像分析结果 |
| Redis | 登录 Token、权限缓存、号源缓存、分布式锁、问诊上下文、接口限流 |
| Kafka | CT 影像分析任务、影像分析结果、AI 调用日志、系统通知 |
| MinIO | 存储 DICOM、NIfTI、热力图、检查报告、知识库文档 |

---

## 4. 微服务与模块划分

### 4.1 推荐实施方案：模块化单体 + Python 影像微服务

考虑到项目是课程/实训/毕业设计性质，不建议一开始拆成大量微服务。推荐采用：

```text
一个 Vue 前端项目
一个 Java Spring Boot 后端项目
一个 Python FastAPI 影像服务项目
PostgreSQL + Redis + Kafka + MinIO
```

Java 后端内部按照业务模块分包，保留未来拆分为微服务的可能。

---

### 4.2 Java 后端模块结构

```text
smart-medical-backend/
├── common/                         # 通用工具、统一返回、异常处理、常量、枚举
├── auth/                           # 登录认证、JWT、Spring Security、当前登录用户上下文
│   ├── controller/
│   ├── service/
│   ├── security/
│   ├── dto/
│   └── vo/
├── system/                         # 系统管理、角色、菜单、权限、字典、操作日志
├── patient/                        # 患者档案、过敏史、既往史、患者历史资料
├── doctor/                         # 医生档案、科室归属、职称、排班关联
├── registration/                   # 挂号、退号、号源、接诊队列
├── outpatient/                     # 门诊接诊、病历首页、诊断、门诊确诊
├── inspection/                     # 检查/检验/处置申请和结果
├── pharmacy/                       # 药品、库存、发药、退药
├── payment/                        # 收费、退费、订单、费用明细
├── file/                           # MinIO 文件服务、附件元数据、临时访问链接
├── image/                          # CT 文件记录、影像分析任务、Python 服务调用、Kafka 结果回传
└── ai/                             # AI 能力中心
    ├── controller/                 # AI 接口入口：问诊、导诊、病历草稿、辅助诊断、影像解释
    ├── application/                # AI 编排层
    │   ├── agent/                  # 各类 Agent 编排服务
    │   ├── tool/                   # Tool Calling 适配层
    │   ├── rag/                    # 检索增强流程
    │   ├── memory/                 # 会话记忆、上下文管理
    │   └── log/                    # AI 调用日志、审计日志
    ├── domain/                     # AI 领域对象：问诊结果、导诊结果、风险等级等
    ├── infrastructure/             # Spring AI、pgvector、Redis、Kafka、MinIO、Python、MCP 接入层
    └── config/                     # AI 相关配置类
```

说明：

```text
1. 顶层主骨架仍按医院业务域拆分，适配患者端、医生端和管理端共用同一套后端能力。
2. 普通业务模块内部仍采用 controller / service / mapper / entity / dto / vo 的常规分层方式。
3. ai 模块不再简单平铺 chat / triage / record，而是单独做成“能力中心”，避免后续 Agent、Tool Calling、RAG、Memory、MCP 混在一起。
4. image 模块负责 Java 侧影像业务调度；Python 只负责算法推理，二者通过 HTTP / Kafka 协作。
```

---

### 4.3 Python 影像服务模块结构

```text
smart-medical-image-ai/
├── app/                       # 在线影像推理服务
│   ├── main.py
│   ├── api/
│   │   ├── router.py
│   │   ├── image_api.py
│   │   └── schemas.py
│   ├── inference/
│   │   ├── image_processor.py
│   │   ├── hemorrhage_detector.py
│   │   ├── resnet_model.py
│   │   └── gradcam.py
│   ├── kafka/
│   │   ├── consumer.py
│   │   └── producer.py
│   ├── storage/
│   │   └── minio_client.py
│   └── utils/
│       ├── logger.py
│       └── response.py
│
├── training/                  # 离线模型训练模块
│   ├── dataset.py
│   ├── preprocess.py
│   ├── train.py
│   ├── evaluate.py
│   ├── config.yaml
│   └── metrics.py
│
├── models/                    # 训练好的模型权重
│   └── hemorrhage_resnet50_v1.pth
│
├── scripts/                   # 数据处理脚本
│   ├── dicom_to_nifti.py
│   ├── build_dataset_index.py
│   └── split_train_val.py
│
└── requirements.txt
```

说明：

```text
training/：只负责离线训练模型，不参与线上接口请求
models/：保存训练后得到的 .pth 权重
app/inference/：线上服务加载 .pth 权重完成推理
app/api/：给 Java 提供 HTTP 接口
app/kafka/：消费 Java 发送的影像分析任务
```

---

## 5. Spring AI Agent 设计

### 5.1 Agent 总体设计

本项目的 Agent 不放在 Python 中实现，而是放在 Java 后端中基于 Spring AI 实现。Python 服务只作为 CT 影像算法工具，由 Java 调用。

Agent 的核心构成为：

```text
Prompt + ChatModel + ChatMemory + Tools + RAG + 业务上下文
```

其中：

```text
Prompt：定义角色、安全边界、输出格式
ChatModel：调用大模型 API，例如 DeepSeek、通义千问、OpenAI 等
ChatMemory：保存多轮问诊上下文，可使用 Redis 或数据库
Tools：调用患者信息、病历、过敏史、科室推荐、CT 影像分析等工具
RAG：检索医学知识库，减少大模型幻觉
业务上下文：由 Java 查询 PostgreSQL 后注入 Agent
```

---

### 5.2 Agent 类型划分

| Agent | 作用 |
|---|---|
| 智能问诊 Agent | 与患者多轮对话，理解主诉，追问关键症状 |
| 智能导诊 Agent | 根据症状和规则推荐科室 |
| 病历生成 Agent | 根据问诊记录、医生输入和检查结果生成病历草稿 |
| 辅助诊断 Agent | 综合病历、检查结果、影像分析结果生成辅助诊断建议 |
| 影像结果解释 Agent | 将 Python 模型输出的结构化结果解释成医生/患者可读文本 |
| 药品安全提醒 Agent | 根据过敏史、既往史和处方进行风险提醒 |

---

### 5.3 Agent 工具集设计

Spring AI Tool Calling 可封装以下工具：

```text
PatientInfoTool：查询患者基本信息
AllergyHistoryTool：查询患者过敏史
MedicalRecordTool：查询历史病历
DepartmentRecommendTool：根据规则推荐科室
KnowledgeSearchTool：检索医学知识库
CtImageAnalysisTool：查询 CT 影像分析结果或触发影像分析任务
PrescriptionSafetyTool：检查处方与过敏史、禁忌信息是否冲突
```

示例工具说明：

```java
@Component
public class CtImageAnalysisTool {

    private final CtAnalysisService ctAnalysisService;

    public CtImageAnalysisTool(CtAnalysisService ctAnalysisService) {
        this.ctAnalysisService = ctAnalysisService;
    }

    @Tool(description = "查询患者脑部 CT 影像分析结果，包括是否疑似脑出血、置信度、异常切片和热力图地址")
    public CtAnalysisResult getCtAnalysisResult(Long taskId) {
        return ctAnalysisService.getAnalysisResult(taskId);
    }
}
```

---

### 5.4 智能问诊 Agent 流程

```text
1. 患者进入智能问诊页面
2. 前端发送患者输入到 Java 后端
3. Java 查询患者基本信息、过敏史、既往史和历史病历摘要
4. Java 将上下文注入 Spring AI Prompt
5. Spring AI 调用大模型 API
6. Agent 必要时调用 RAG 知识库和科室推荐工具
7. Agent 返回回复内容、风险等级、推荐科室和安全提醒
8. Java 保存问诊消息和问诊摘要
9. 前端通过普通响应或 SSE 展示 AI 回复
```

---

### 5.5 智能导诊 Agent 流程

```text
1. 患者描述症状
2. Agent 判断症状是否完整
3. 如果信息不足，继续追问疼痛部位、持续时间、伴随症状等
4. 如果信息足够，调用科室推荐工具
5. 检索科室分诊规则知识库
6. 返回推荐科室、备选科室、推荐原因和风险提示
7. 患者可跳转至挂号模块
```

---

### 5.6 AI 病历生成 Agent 流程

```text
1. 医生进入患者病历页面
2. 系统加载患者 AI 问诊记录、主诉、既往史、过敏史、检查结果
3. 医生点击“AI 生成病历草稿”
4. Spring AI 生成主诉、现病史、既往史、初步诊断建议、检查建议
5. 医生审核、修改后保存
6. 系统记录 AI 生成内容和医生修改记录
```

注意：AI 只能生成草稿，不能直接确诊，不能直接开处方。

---

### 5.7 影像结果解释 Agent 流程

```text
1. Python 影像服务完成 CT 推理
2. Java 保存结构化结果：是否出血、置信度、异常切片、热力图地址
3. 医生打开影像分析结果页面
4. Spring AI Agent 读取患者主诉、病历和 CT 分析结果
5. Agent 生成辅助说明
6. 医生查看结果并人工确认
```

示例结构化结果：

```json
{
  "hasHemorrhage": true,
  "confidence": 0.91,
  "riskLevel": "high",
  "abnormalSlices": [12, 13, 14],
  "heatmapUrl": "minio://medical-ct-heatmap/xxx.png",
  "modelName": "hemorrhage_resnet50_v1"
}
```

Agent 解释示例：

```text
AI 影像模型提示该 CT 存在疑似脑出血高风险区域，置信度为 91%，异常区域主要集中在第 12-14 层切片。请结合患者临床表现、影像科报告和医生判断进一步确认。
```

---

## 6. 医学知识库 RAG 设计

### 6.1 知识库内容

医学知识库主要用于辅助问诊、导诊、处方安全提醒和病历生成。

建议包括：

```text
常见疾病介绍
科室分诊规则
药品禁忌说明
检查检验项目说明
脑出血相关医学知识
医院诊疗流程说明
医学指南文档
课程项目自定义问答数据
```

---

### 6.2 RAG 流程

```text
1. 管理员上传医学知识文档
2. Java 文件服务保存原始文档到 MinIO
3. Spring AI 对文档进行切分
4. 调用 Embedding 模型生成向量
5. 保存向量到向量库
6. 用户提问时进行相似度检索
7. 将检索结果注入 Prompt
8. 大模型基于检索内容生成回答
9. 返回回答和引用来源
```

---

### 6.3 向量库选择

因为本项目主数据库使用 PostgreSQL，推荐使用：

```text
PostgreSQL + pgvector
```

优点：

```text
1. 和主数据库技术栈统一
2. 不需要额外部署 Milvus、Elasticsearch 等服务
3. 适合学生项目和中小规模知识库
4. 后续可以平滑迁移到专业向量数据库
```

---

## 7. CT 影像分析设计

### 7.1 支持的上传格式

本项目 CT 影像分析模块仅支持医学影像原始格式：

```text
.dcm       单个 DICOM 切片
.nii       NIfTI 三维医学影像
.nii.gz    压缩 NIfTI 三维医学影像
.zip       DICOM 序列压缩包，内部必须是多个 .dcm 文件
```

暂不支持：

```text
.jpg
.jpeg
.png
.bmp
普通截图
普通照片
```

不支持 JPG/PNG 的原因：普通图片缺少 HU 值、Rescale Slope、Rescale Intercept、切片厚度、像素间距等关键医学影像信息，无法保证脑窗预处理和模型推理结果的可靠性。

---

### 7.2 上传入口设计

系统支持患者端和医生端两种上传入口：

| 上传端 | 场景 | 结果使用方式 |
|---|---|---|
| 患者端 | 线上问诊前上传已有 CT 文件 | AI 初步风险提示，医生后续确认 |
| 医生端 | 接诊过程中上传 CT 文件 | 辅助医生判断，医生确认后关联病历 |

最终原则：

```text
患者和医生都可以上传 CT，但 AI 分析结果必须由医生确认后才能进入正式病历。
```

---

### 7.3 MinIO 文件存储设计

MinIO 用于存储大文件，PostgreSQL 只保存文件记录和访问路径。

建议分桶：

```text
medical-ct-original      原始 DICOM / NIfTI 文件
medical-ct-processed     预处理后的中间结果，可选
medical-ct-heatmap       Grad-CAM 热力图
medical-report           检查报告附件
knowledge-docs           医学知识库文档
```

CT 文件上传流程：

```text
1. 前端上传 CT 文件到 Java 后端
2. Java 校验文件格式
3. Java 上传原始文件到 MinIO
4. PostgreSQL 保存文件记录
5. Java 创建 CT 影像分析任务
6. Java 发送 Kafka 消息通知 Python 分析
```

---

### 7.4 CT 影像分析异步流程

```text
1. 患者或医生上传 DICOM / NIfTI 文件
2. Java 保存文件到 MinIO
3. Java 创建影像分析任务，状态为 pending
4. Java 向 Kafka 发送 medical.image.analysis.request 消息
5. Python 影像服务消费任务
6. Python 从 MinIO 下载原始文件
7. Python 读取 DICOM / NIfTI
8. 进行 HU 转换和脑窗预处理
9. 加载训练好的 ResNet-50 权重
10. 模型推理判断是否疑似脑出血
11. 使用 Grad-CAM 生成热力图
12. Python 上传热力图到 MinIO
13. Python 向 Kafka 发送 medical.image.analysis.result 消息
14. Java 消费结果并更新 PostgreSQL
15. 前端展示分析结果
16. 医生人工确认
```

---

### 7.5 Kafka Topic 设计

| Topic 名称 | 生产者 | 消费者 | 说明 |
|---|---|---|---|
| medical.image.analysis.request | Java | Python | CT 影像分析请求 |
| medical.image.analysis.result | Python | Java | CT 影像分析结果 |
| medical.ai.chat.log | Java | Java | AI 问诊日志异步归档 |
| medical.audit.log | Java | Java | 操作审计日志 |
| medical.notification | Java/Python | Java | 系统通知消息 |

影像分析请求示例：

```json
{
  "taskId": 10001,
  "patientId": 20001,
  "visitId": 30001,
  "fileId": 40001,
  "bucket": "medical-ct-original",
  "objectKey": "2026/06/10/patient_20001/ct_001.nii.gz",
  "imageType": "NIFTI",
  "analysisType": "ICH_DETECTION",
  "createTime": "2026-06-10 10:30:00"
}
```

影像分析结果示例：

```json
{
  "taskId": 10001,
  "patientId": 20001,
  "hasHemorrhage": true,
  "confidence": 0.91,
  "riskLevel": "high",
  "abnormalSlices": [12, 13, 14],
  "heatmapBucket": "medical-ct-heatmap",
  "heatmapObjectKey": "2026/06/10/patient_20001/heatmap_001.png",
  "modelName": "hemorrhage_resnet50_v1",
  "status": "success",
  "finishTime": "2026-06-10 10:31:20"
}
```

---

## 8. 医学影像模型训练设计

### 8.1 训练与线上推理的关系

本项目医学影像 AI 采用：

```text
离线训练 + 在线推理 + Spring AI Agent 解释
```

不能把训练过程放在线上系统请求里。训练只在开发阶段或模型更新阶段离线执行；系统运行时只加载训练好的 `.pth` 权重进行推理。

---

### 8.2 数据集格式

当前使用 CQ500 数据集，主要涉及：

#### 1. DICOM 格式

```text
.dcm
```

每个 `.dcm` 文件表示一次 CT 检查中的一个 2D 切片，文件中包含图像像素和医学元数据，例如 Rescale Slope、Rescale Intercept、Pixel Spacing、Slice Thickness 等。

#### 2. NIfTI 格式

```text
.nii
.nii.gz
```

NIfTI 常用于科研和深度学习，可以将一个患者的多个 DICOM 切片合并成一个 3D 体数据，例如：

```text
512 x 512 x 30
```

表示 30 层切片，每层分辨率 512 x 512。

#### 3. 标签文件 reads.csv

`reads.csv` 保存医生标注的标准答案，例如是否存在颅内出血、脑实质出血、蛛网膜下腔出血等。

示例：

```text
patient_id,ICH,IPH,SAH
patient_001,1,1,0
patient_002,0,0,0
```

---

### 8.3 脑窗预处理

CT 原始 HU 值范围较大：

```text
空气：约 -1000 HU
脑组织：约 +20 到 +45 HU
急性脑出血：约 +60 到 +80 HU
骨头：+1000 HU 以上
```

为了减少骨头等高亮区域对模型的干扰，本项目采用脑窗技术：

```text
窗宽 Window Width = 80
窗位 Window Level = 40
保留范围约为 [0, 80] HU
```

处理流程：

```text
1. 读取原始像素
2. 根据 Rescale Slope / Rescale Intercept 转换为 HU 值
3. 应用脑窗裁剪
4. 归一化到 [0, 1]
5. 调整尺寸为模型输入大小
6. 转换为单通道张量
```

---

### 8.4 模型训练方案

推荐模型：

```text
ResNet-50 二分类模型
```

改造方式：

```text
1. 将第一层卷积改为单通道输入，适配灰度 CT 图像
2. 将最后全连接层改为二分类输出
3. 输出是否疑似脑出血的概率
```

训练配置建议：

```text
损失函数：BCEWithLogitsLoss 或 CrossEntropyLoss
优化器：AdamW
学习率调度：Cosine Annealing 余弦退火
评价指标：Accuracy、Precision、Recall、F1、AUC
可视化：Grad-CAM
```

余弦退火说明：

```text
训练初期使用较大学习率，加快模型收敛；训练后期按照余弦曲线逐步降低学习率，使参数更新更加稳定，提高模型泛化能力。
```

---

### 8.5 训练流程

```text
1. 准备 CQ500 数据集
2. 读取 DICOM / NIfTI 文件
3. 根据 reads.csv 构建样本和标签
4. 完成 HU 转换和脑窗预处理
5. 划分训练集和验证集
6. 构建 ResNet-50 模型
7. 使用 PyTorch 进行训练
8. 使用验证集评估模型效果
9. 保存最优模型权重 .pth
10. 将权重放入 models/ 目录
11. 在线推理服务加载该权重
```

---

### 8.6 线上推理流程

```text
1. Python 服务启动时加载 hemorrhage_resnet50_v1.pth
2. 接收到影像分析任务
3. 从 MinIO 下载 CT 文件
4. 读取影像并进行脑窗预处理
5. 输入模型进行推理
6. 输出是否疑似脑出血和置信度
7. 使用 Grad-CAM 生成热力图
8. 上传热力图到 MinIO
9. 返回结构化结果给 Java
```

---

## 9. 核心业务模块设计

### 9.1 患者模块

功能：

```text
患者注册与登录
患者基本信息维护
过敏史维护
既往史维护
历史病历查看
智能问诊入口
CT 影像上传入口
AI 问诊结果查看
AI 影像分析结果查看
```

---

### 9.2 医生模块

功能：

```text
医生登录
查看待接诊患者
查看患者基本信息
查看患者 AI 问诊摘要
查看患者历史病历
填写病历首页
开立检查/检验/处置申请
查看检查结果
查看 CT 影像分析结果
查看 AI 辅助诊断建议
门诊确诊
开立处方
结束看诊
```

---

### 9.3 挂号模块

功能：

```text
查询科室
查询医生排班
查询号源
创建挂号记录
退号
查询挂号记录
挂号状态管理
```

高并发控制：

```text
Redis 缓存号源
Redis 分布式锁控制并发
PostgreSQL 事务保存挂号记录
Kafka 异步记录日志
```

---

### 9.4 病历模块

功能：

```text
新建病历
编辑病历
保存病历首页
查询历史病历
关联疾病诊断
关联检查结果
关联处方
AI 生成病历草稿
医生确认病历内容
```

---

### 9.5 检查检验模块

功能：

```text
医生开立检查申请
医生开立检验申请
检查科室查看待检查患者
检查登记
检查结果录入
检查附件上传
医生查看检查结果
CT 影像分析任务关联
```

---

### 9.6 药房模块

功能：

```text
药品信息维护
药品库存管理
查看待发药患者
发药
退药
发退药记录查询
库存预警
```

---

### 9.7 缴费模块

功能：

```text
查询患者待缴费项目
创建缴费订单
完成收费
退费
费用记录查询
日结统计
```

缴费属于强事务场景，由 Java 后端和 PostgreSQL 事务控制。

---

## 10. 数据库规划

本章节只做数据表分类，具体字段在后续数据库文档中单独设计。

当前数据库规划采用“认证与身份核心表先落地”的分阶段方案。

### 10.1 第一阶段认证与身份核心表

第一阶段先落 6 张核心表：

```text
sys_user                  统一登录账户表
sys_role                  角色表
sys_user_role             用户角色关联表
patient                   患者表
department                科室表
doctor                    医生表
```

说明：

- `sys_user` 负责账号本身
- `sys_role / sys_user_role` 负责角色扩展
- `doctor.department_id` 负责医生所属科室
- 后续“不同科室看到不同数据”主要依赖科室字段和数据权限，而不是把每个科室都拆成独立角色

---

### 10.2 后续扩展业务表

当登录底座稳定后，再逐步扩展以下业务表：

```text
doctor_schedule           排班表
registration              挂号表
medical_record            病历表
check_request             检查申请表
inspection_request        检验申请表
disposal_request          处置申请表
prescription              处方表
drug_info                 药品表
payment_order             缴费订单表
payment_item              缴费明细表
```

---

### 10.3 AI 相关表

```text
ai_chat_session           AI 问诊会话表
ai_chat_message           AI 问诊消息表
ai_triage_result          AI 导诊结果表
ai_medical_record_draft   AI 病历草稿表
ai_diagnosis_advice       AI 辅助诊断建议表
ai_call_log               AI 调用日志表
knowledge_document        医学知识库文档表
knowledge_chunk           医学知识库分片表
knowledge_vector          医学知识库向量表，可用 pgvector
```

---

### 10.3 影像相关表

```text
ct_image_file             CT 文件记录表
ct_analysis_task          CT 影像分析任务表
ct_analysis_result        CT 影像分析结果表
file_record               通用文件记录表
```

---

## 11. Redis 使用设计

### 11.1 Redis 使用场景

```text
登录 Token 缓存
用户权限缓存
科室、医生、药品等基础数据缓存
医生排班和号源缓存
挂号分布式锁
AI 问诊上下文缓存
接口限流
防重复提交
Kafka 消费幂等标记
```

---

### 11.2 Redis Key 设计示例

```text
login:token:{token}
user:permission:{userId}
department:list
doctor:schedule:{doctorId}:{date}
registration:quota:{doctorId}:{date}:{noon}
lock:registration:{doctorId}:{date}:{noon}
ai:chat:session:{sessionId}
ai:chat:memory:{sessionId}
limit:api:{userId}:{apiName}
kafka:consume:message:{messageId}
```

---

## 12. 安全设计

### 12.1 登录认证与权限控制

系统采用 RBAC 权限模型：

```text
用户 -> 角色 -> 权限
```

角色包括：

```text
患者
门诊医生
检查医生
检验医生
药房人员
挂号收费员
管理员
```

---

### 12.2 医疗数据安全

```text
患者隐私数据必须经过权限校验
Python 影像服务不直接暴露给前端
前端不能直接访问 MinIO 私有文件
Java 后端统一生成临时访问链接
AI 调用记录需要保存审计日志
病历访问和修改需要记录操作日志
```

---

### 12.3 AI 安全边界

必须明确：

```text
AI 不能替代医生诊断
AI 不能直接开处方
AI 不能直接修改正式病历
AI 生成病历只能作为草稿
AI 影像分析仅作为辅助参考
最终诊断、处方和病历确认必须由医生完成
高风险症状必须提示及时就医或急诊
```

---

## 13. 开发阶段计划

### 第一阶段：基础环境搭建

```text
搭建 Vue 前端项目
搭建 Spring Boot 后端项目
接入 Spring AI
搭建 Python FastAPI 影像服务
配置 PostgreSQL
配置 Redis
配置 Kafka
配置 MinIO
配置统一返回格式和异常处理
配置 Knife4j / Swagger
```

---

### 第二阶段：基础业务模块开发

```text
登录认证
角色权限管理
患者管理
医生管理
科室管理
排班管理
挂号管理
病历管理
检查/检验申请
处方管理
药房发药
缴费管理
```

---

### 第三阶段：Spring AI 问诊与导诊开发

```text
接入大模型 API
设计智能问诊 Prompt
实现问诊会话管理
实现患者上下文注入
实现过敏史和历史病历工具调用
实现智能导诊 Agent
实现 AI 问诊记录保存
前端实现流式输出
医生端展示 AI 问诊摘要
```

---

### 第四阶段：医学知识库 RAG 开发

```text
准备医学知识文档
上传知识库文档到 MinIO
文档切分
生成向量
保存到 PostgreSQL pgvector
实现知识检索工具
接入 Spring AI Agent
回答时返回知识来源
```

---

### 第五阶段：CT 模型离线训练

```text
整理 CQ500 数据集
读取 DICOM / NIfTI 文件
读取 reads.csv 标签
实现 HU 转换和脑窗预处理
构建 PyTorch Dataset
训练 ResNet-50 二分类模型
使用 AdamW 和余弦退火优化训练
评估 Accuracy、Recall、F1、AUC
保存最优 .pth 权重
```

---

### 第六阶段：CT 在线推理服务开发

```text
Python 服务加载 .pth 权重
实现 DICOM / NIfTI 文件读取
实现脑窗预处理
实现模型推理
实现 Grad-CAM 热力图生成
实现 MinIO 文件读取和上传
实现 Kafka 消费影像分析任务
实现分析结果返回 Java
```

---

### 第七阶段：系统联调与优化

```text
前后端接口联调
Java 与 Python 服务联调
Kafka 消息联调
Redis 缓存和锁测试
MinIO 文件上传下载测试
Spring AI Agent 效果优化
RAG 检索效果优化
CT 影像分析流程测试
权限测试
异常测试
```

---

### 第八阶段：部署与文档整理

```text
Docker 部署 PostgreSQL
Docker 部署 Redis
Docker 部署 Kafka
Docker 部署 MinIO
部署 Java 后端
部署 Python 影像服务
部署 Vue 前端
整理接口文档
整理数据库文档
整理项目说明书
准备演示数据
准备答辩 PPT
```

---

## 14. 推荐开发优先级

### 第一优先级：最小可运行闭环

```text
登录认证
患者管理
医生管理
科室排班
挂号
医生接诊
病历填写
Spring AI 智能问诊
Spring AI 智能导诊
AI 问诊摘要展示
```

### 第二优先级：AI 增强能力

```text
RAG 医学知识库
AI 病历草稿生成
AI 辅助诊断建议
药品安全提醒
```

### 第三优先级：CT 影像分析

```text
CT 文件上传
MinIO 存储
Kafka 异步任务
Python 模型推理
Grad-CAM 热力图
医生确认影像结果
```

---

## 15. 项目亮点

```text
1. 使用 Spring Boot + Spring AI 构建 Java 主导的医疗 Agent 系统
2. 使用 Spring AI Tool Calling 让 Agent 调用患者信息、病历、知识库和 CT 影像分析工具
3. 使用 RAG 医学知识库降低大模型幻觉，提高回答可靠性
4. 使用 Python FastAPI + PyTorch 构建独立 CT 影像算法服务
5. 使用 CQ500 数据集进行离线训练，线上只加载 .pth 权重推理
6. 使用脑窗预处理突出脑组织和出血区域，减少骨骼干扰
7. 使用 Grad-CAM 生成病灶热力图，提高模型可解释性
8. 使用 Kafka 解耦耗时影像分析任务
9. 使用 Redis 处理高并发挂号锁、缓存和问诊上下文
10. 使用 MinIO 存储医学影像和热力图，避免数据库存储大文件
11. 明确 AI 安全边界，所有 AI 结果必须由医生确认
```

---

## 16. 风险与解决方案

| 风险 | 说明 | 解决方案 |
|---|---|---|
| AI 回答不准确 | 大模型可能产生幻觉 | 使用 RAG、固定 Prompt、安全边界、医生审核 |
| 医疗安全风险 | AI 不能直接确诊或开药 | AI 仅生成建议，医生确认后生效 |
| 影像模型效果不足 | 数据集处理和训练难度较高 | 先完成流程，再优化模型；保留评估指标 |
| JPG/PNG 不支持 | 患者可能只有截图 | 明确系统只支持 DICOM/NIfTI，减少误判风险 |
| 微服务过度复杂 | 多服务部署和调试成本高 | 第一阶段采用模块化单体 + Python 影像服务 |
| Kafka 增加复杂度 | 消息队列调试成本较高 | 只用于 CT 影像分析等必要异步场景 |
| Redis 锁失效 | 并发挂号可能出现超卖 | 设置锁过期时间，使用唯一标识释放锁，数据库兜底 |
| Python 服务异常 | 影像分析失败 | Java 保存失败状态，支持重试，不影响基础业务 |
| 数据隐私问题 | 患者病历和影像敏感 | 权限控制、最小化传输、访问日志审计 |

---

## 17. 最终架构结论

本项目最终采用：

```text
Java Spring Boot + Spring AI Agent + Python FastAPI 影像算法服务
```

Java 后端负责医院业务系统、Agent 编排、RAG 检索、工具调用和 AI 结果解释；Python 服务只负责 CT 影像预处理、模型推理和热力图生成。医学影像模型采用离线训练、在线推理方式，训练代码单独放在 Python 项目的 `training/` 目录中，线上服务只加载训练好的 `.pth` 权重进行推理。

最终系统形成：

```text
业务流程由 Java 保证稳定性
智能问诊由 Spring AI 实现
CT 看图由 Python 模型完成
结果解释由 Spring AI Agent 完成
文件存储由 MinIO 完成
异步任务由 Kafka 解耦
高并发和缓存由 Redis 支撑
核心数据由 PostgreSQL 持久化
```
