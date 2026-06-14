# 智慧云脑诊疗 Agent 平台接口文档（草案）

> 用途：根据当前后端技术栈、模块结构和数据库设计，整理一份可直接指导后端接口开发与前后端联调的设计级接口文档。  
> 说明：本文档是“接口设计文档”，不是运行时 Swagger 导出结果。当前 `auth` 模块已有真实接口雏形，其他模块按统一 REST 风格设计。

---

## 1. 技术前提

当前后端技术栈：

```text
Java 17
Spring Boot 3.2.4
Spring MVC
MyBatis-Plus
PostgreSQL
Redis
Kafka
MinIO
Spring AI 1.0.0-M7
JWT
```

当前仓库模块口径：

```text
auth
system
patient
doctor
registration
outpatient
inspection
pharmacy
payment
file
image
ai
```

---

## 2. 接口设计总约定

### 2.1 基础路径

统一前缀建议：

```text
/api
```

示例：

```text
/api/auth/login
/api/patients/{id}
/api/registrations
/api/ai/chat/sessions
```

### 2.2 认证方式

- 登录后返回 JWT `accessToken`
- 后续常规接口通过请求头传递：

```http
Authorization: Bearer {accessToken}
```

- **特殊说明（SSE等流式接口）：** 对于无法自定义请求头的浏览器 `EventSource` 请求，允许通过 URL Query 参数传递：`?accessToken={accessToken}`。

### 2.3 统一返回结构

建议所有业务接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-11T10:00:00Z"
}
```

分页接口建议：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "pageNo": 1,
    "pageSize": 10,
    "total": 100
  },
  "timestamp": "2026-06-11T10:00:00Z"
}
```

### 2.4 常用状态码约定

| 状态码 | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 业务冲突，例如重复挂号、库存不足 |
| 500 | 系统异常 |

### 2.5 时间与分页参数

- 时间统一使用 ISO-8601 或后端约定的 `yyyy-MM-dd HH:mm:ss`
- 分页统一使用：
  - `pageNo`
  - `pageSize`

### 2.6 当前登录用户上下文建议

登录态上下文建议能解析出：

```text
userId
username
role
userType
bizId
sessionId
```

这与当前 `auth` 模块返回结构保持一致。

---

## 3. 当前已存在的认证接口

根据当前仓库 `AuthController`，已经存在以下接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register`| 账号自主注册 (✔ 已实现) |
| POST | `/api/auth/login` | 登录 (✔ 已实现) |
| POST | `/api/auth/refresh` | 刷新 access token (✔ 已实现) |
| POST | `/api/auth/logout` | 退出登录 (✔ 已实现) |

### 3.0 `POST /api/auth/register`

**作用：** 患者自主注册账号，同时生成登录账号和基本档案。

**请求体：**

```json
{
  "username": "zhangsan_01",
  "password": "mySecurePassword123",
  "realName": "张三",
  "phone": "13800138000",
  "idCard": "110105199001011234",
  "gender": "MALE"
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": "2026-06-11T10:00:00Z"
}
```

### 3.1 `POST /api/auth/login`

**请求体：**

```json
{
  "username": "doctor001",
  "password": "123456"
}
```

**响应体：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "xxx",
    "refreshToken": "xxx",
    "tokenType": "Bearer",
    "accessTokenExpiresAt": "2026-06-11T12:00:00Z",
    "refreshTokenExpiresAt": "2026-06-18T12:00:00Z",
    "userId": 1001,
    "username": "doctor001",
    "role": "DOCTOR",
    "userType": "DOCTOR",
    "bizId": 3001,
    "sessionId": "session-uuid",
    "refreshTokenId": "refresh-uuid"
  },
  "timestamp": "2026-06-11T10:00:00Z"
}
```

### 3.2 `POST /api/auth/refresh`

**请求体：**

```json
{
  "refreshToken": "xxx"
}
```

### 3.3 `POST /api/auth/logout`

**请求头：**

```http
Authorization: Bearer {accessToken}
```

---

## 4. 认证与系统治理接口

## 4.1 用户上下文接口

| 方法 | 路径 | 说明 | 角色 |
|---|---|---|---|
| GET | `/api/auth/me` | 获取当前登录用户信息 (✔ 已实现) | 已登录用户 |

**返回关键字段：**

- `userId`
- `username`
- `realName`
- `userType`
- `role`
- `bizId`

---

## 4.2 角色权限接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/system/roles` | 查询角色列表 | ADMIN | `sys_role` |
| POST | `/api/system/roles` | 新增角色 | ADMIN | `sys_role` |
| PUT | `/api/system/roles/{id}` | 修改角色 | ADMIN | `sys_role` |
| GET | `/api/system/permissions` | 查询权限列表 | ADMIN | `sys_permission` |
| POST | `/api/system/roles/{id}/permissions` | 配置角色权限 | ADMIN | `sys_role_permission` |
| GET | `/api/system/menus` | 查询菜单树 | ADMIN | `sys_menu` |
| GET | `/api/system/operation-logs` | 查询操作日志 | ADMIN | `sys_operation_log` |

---

## 5. 患者与医生基础接口

## 5.1 患者接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/patients` | 新增患者档案 (✔ 已实现) | ADMIN / REGISTRATION_CLERK | `patient` |
| GET | `/api/patients/{id}` | 查询患者详情 (✔ 已实现) | PATIENT / DOCTOR / ADMIN | `patient` |
| PUT | `/api/patients/{id}` | 修改患者信息 (✔ 已实现) | PATIENT / ADMIN | `patient` |
| GET | `/api/patients` | 条件分页查询患者 (✔ 已实现) | DOCTOR / ADMIN | `patient` |
| GET | `/api/patients/{id}/records` | 查询患者历史病历 | DOCTOR / PATIENT | `medical_record` |

**新增患者请求体建议：**

```json
{
  "name": "张三",
  "gender": "MALE",
  "birthDate": "1990-01-01",
  "phone": "13800000000",
  "idCard": "110101199001010011",
  "bloodType": "A",
  "allergySummary": "青霉素过敏",
  "historySummary": "高血压病史3年"
}
```

---

## 5.2 科室接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/departments` | 查询科室列表 (✔ 已实现) | 全部 | `department` |
| GET | `/api/departments/tree` | 查询科室树 | 全部 | `department` |
| POST | `/api/departments` | 新增科室 (✔ 已实现) | ADMIN | `department` |
| PUT | `/api/departments/{id}` | 修改科室 (✔ 已实现) | ADMIN | `department` |

---

## 5.3 医生接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/doctors` | 查询医生列表 (✔ 已实现) | 全部 | `doctor` |
| GET | `/api/doctors/{id}` | 查询医生详情 (✔ 已实现) | 全部 | `doctor` |
| POST | `/api/doctors` | 新增医生档案 (✔ 已实现) | ADMIN | `doctor` |
| PUT | `/api/doctors/{id}` | 修改医生档案 (✔ 已实现) | ADMIN | `doctor` |
| GET | `/api/doctors/{id}/schedules` | 查询某医生排班 | 全部 | `doctor_schedule` |

---

## 6. 挂号与排班接口

## 6.1 排班接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/schedules` | 分页查询排班 (✔ 已实现) | 全部 | `doctor_schedule` |
| POST | `/api/schedules` | 新增排班 (✔ 已实现) | ADMIN | `doctor_schedule` |
| PUT | `/api/schedules/{id}` | 修改排班 (✔ 已实现) | ADMIN | `doctor_schedule` |
| POST | `/api/schedules/{id}/close` | 停诊 / 关闭号源 (✔ 已实现) | ADMIN | `doctor_schedule` |

**查询参数建议：**

- `departmentId`
- `doctorId`
- `scheduleDate`
- `timeSlot`
- `pageNo`
- `pageSize`

---

## 6.2 挂号接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/registrations` | 创建挂号 | PATIENT / REGISTRATION_CLERK | `registration` |
| GET | `/api/registrations/{id}` | 查询挂号详情 | PATIENT / DOCTOR / ADMIN | `registration` |
| GET | `/api/registrations/my` | 查询当前患者挂号记录 | PATIENT | `registration` |
| GET | `/api/registrations` | 分页查询挂号记录 | DOCTOR / ADMIN | `registration` |
| POST | `/api/registrations/{id}/cancel` | 退号 | PATIENT / REGISTRATION_CLERK | `registration` |

**创建挂号请求体建议：**

```json
{
  "patientId": 20001,
  "scheduleId": 30001,
  "visitDate": "2026-06-12",
  "timeSlot": "AM"
}
```

**创建挂号响应关键字段：**

- `registrationId`
- `registrationNo`
- `queueNo`
- `feeAmount`
- `status`

---

## 6.3 接诊队列接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/queue/doctor/me` | 查询当前医生待诊队列 | DOCTOR | `visit_queue` |
| POST | `/api/queue/{registrationId}/call` | 叫号 | DOCTOR | `visit_queue` |
| POST | `/api/queue/{registrationId}/skip` | 过号 | DOCTOR | `visit_queue` |
| POST | `/api/queue/{registrationId}/finish` | 完成接诊队列状态 | DOCTOR | `visit_queue` |

---

## 7. 门诊接诊与病历接口

## 7.1 病历接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/outpatient/records` | 新建病历 | DOCTOR | `medical_record` |
| GET | `/api/outpatient/records/{id}` | 查询病历详情 | DOCTOR / PATIENT | `medical_record` |
| PUT | `/api/outpatient/records/{id}` | 编辑病历 | DOCTOR | `medical_record` |
| POST | `/api/outpatient/records/{id}/confirm` | 确认病历 | DOCTOR | `medical_record` |
| GET | `/api/outpatient/records` | 分页查询病历 | DOCTOR / ADMIN | `medical_record` |

**新建病历请求体建议：**

```json
{
  "registrationId": 50001,
  "patientId": 20001,
  "doctorId": 30001,
  "departmentId": 40001,
  "chiefComplaint": "头痛3天，加重1天",
  "presentIllness": "患者3天前开始头痛，伴恶心",
  "pastHistory": "高血压病史3年",
  "allergyHistory": "青霉素过敏",
  "physicalExam": "血压150/95mmHg",
  "preliminaryDiagnosis": "待排脑血管疾病"
}
```

---

## 7.2 诊断明细接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/outpatient/records/{id}/diagnoses` | 批量保存诊断明细 | DOCTOR | `medical_diagnosis` |
| GET | `/api/outpatient/records/{id}/diagnoses` | 查询诊断明细 | DOCTOR / PATIENT | `medical_diagnosis` |

---

## 8. 检查 / 检验 / 处置接口

## 8.1 检查申请接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/check-requests` | 开立检查申请 | DOCTOR | `check_request` |
| GET | `/api/check-requests/{id}` | 查询检查申请详情 | DOCTOR / ADMIN | `check_request` |
| GET | `/api/check-requests` | 分页查询检查申请 | DOCTOR / ADMIN | `check_request` |
| POST | `/api/check-requests/{id}/cancel` | 取消检查申请 | DOCTOR | `check_request` |

---

## 8.2 检查结果接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/check-results` | 录入检查结果 | INSPECTION_DOCTOR / IMAGE_DOCTOR | `check_result` |
| GET | `/api/check-results/{id}` | 查询检查结果详情 | DOCTOR / PATIENT | `check_result` |
| POST | `/api/check-results/{id}/confirm` | 确认检查报告 | INSPECTION_DOCTOR / IMAGE_DOCTOR | `check_result` |

---

## 8.3 检验申请接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/inspection-requests` | 开立检验申请 | DOCTOR | `inspection_request` |
| GET | `/api/inspection-requests/{id}` | 查询检验申请 | DOCTOR / ADMIN | `inspection_request` |
| GET | `/api/inspection-requests` | 分页查询检验申请 | DOCTOR / ADMIN | `inspection_request` |

---

## 8.4 检验结果接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/inspection-results` | 录入检验结果头 | INSPECTION_DOCTOR | `inspection_result` |
| POST | `/api/inspection-results/{id}/items` | 批量录入检验指标 | INSPECTION_DOCTOR | `inspection_result_item` |
| GET | `/api/inspection-results/{id}` | 查询检验结果详情 | DOCTOR / PATIENT | `inspection_result` |

---

## 8.5 处置申请接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/disposal-requests` | 开立处置申请 | DOCTOR | `disposal_request` |
| GET | `/api/disposal-requests/{id}` | 查询处置申请详情 | DOCTOR / ADMIN | `disposal_request` |
| POST | `/api/disposal-requests/{id}/execute` | 执行处置 | NURSE / DISPOSAL_OPERATOR | `disposal_request` |
| POST | `/api/disposal-requests/{id}/finish` | 完成处置 | NURSE / DISPOSAL_OPERATOR | `disposal_request` |

---

## 9. 处方与药房接口

## 9.1 药品接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/drugs` | 分页查询药品 (✔ 已实现) | DOCTOR / PHARMACY / ADMIN | `drug_info` |
| GET | `/api/drugs/{id}` | 查询药品详情 (✔ 已实现) | DOCTOR / PHARMACY / ADMIN | `drug_info` |
| POST | `/api/drugs` | 新增药品 (✔ 已实现) | PHARMACY / ADMIN | `drug_info` |
| PUT | `/api/drugs/{id}` | 修改药品 (✔ 已实现) | PHARMACY / ADMIN | `drug_info` |
| POST | `/api/drugs/{id}/stock-adjust` | 库存调整 (✔ 已实现) | PHARMACY / ADMIN | `drug_info` |

---

## 9.2 处方接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/prescriptions` | 开立处方 | DOCTOR | `prescription` `prescription_item` |
| GET | `/api/prescriptions/{id}` | 查询处方详情 | DOCTOR / PHARMACY / PATIENT | `prescription` |
| GET | `/api/prescriptions` | 分页查询处方 | DOCTOR / PHARMACY / ADMIN | `prescription` |
| POST | `/api/prescriptions/{id}/cancel` | 取消处方 | DOCTOR | `prescription` |

**开立处方请求体建议：**

```json
{
  "patientId": 20001,
  "registrationId": 50001,
  "medicalRecordId": 60001,
  "doctorId": 30001,
  "departmentId": 40001,
  "items": [
    {
      "drugId": 70001,
      "dosage": "1片",
      "frequency": "每日2次",
      "days": 5,
      "quantity": 10,
      "usageMethod": "口服"
    }
  ],
  "remark": "饭后服用"
}
```

---

## 9.3 发药接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/pharmacy/dispense/pending` | 查询待发药处方 | PHARMACY | `prescription` |
| POST | `/api/pharmacy/dispense` | 发药 | PHARMACY | `drug_dispense_record` |
| POST | `/api/pharmacy/return` | 退药 | PHARMACY | `drug_dispense_record` |
| GET | `/api/pharmacy/dispense-records` | 查询发退药记录 | PHARMACY / ADMIN | `drug_dispense_record` |

---

## 10. 缴费接口

## 10.1 缴费订单接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/payment-orders/pending` | 查询患者待缴费项目 | PATIENT / CASHIER | `payment_order` `payment_item` |
| POST | `/api/payment-orders` | 创建缴费订单 | CASHIER / ADMIN | `payment_order` `payment_item` |
| GET | `/api/payment-orders/{id}` | 查询订单详情 | PATIENT / CASHIER / ADMIN | `payment_order` |
| POST | `/api/payment-orders/{id}/pay` | 完成支付 | PATIENT / CASHIER | `payment_order` |
| POST | `/api/payment-orders/{id}/refund` | 发起退费 | CASHIER / ADMIN | `refund_record` |
| GET | `/api/payment-orders` | 分页查询订单 | CASHIER / ADMIN | `payment_order` |

**支付请求体建议：**

```json
{
  "payChannel": "WECHAT",
  "paidAmount": 36.50
}
```

---

## 10.2 退费接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/refunds/{id}` | 查询退费详情 | CASHIER / ADMIN | `refund_record` |
| GET | `/api/refunds` | 分页查询退费记录 | CASHIER / ADMIN | `refund_record` |

---

## 11. 文件与影像接口

## 11.1 通用文件接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/files/upload` | 上传通用文件 | 已登录用户 | `file_record` |
| GET | `/api/files/{id}` | 查询文件元数据 | 已登录用户 | `file_record` |
| GET | `/api/files/{id}/preview-url` | 获取临时访问链接 | 已登录用户 | `file_record` |

**上传响应关键字段：**

- `fileId`
- `bucketName`
- `objectKey`
- `originalName`
- `fileSize`

---

## 11.2 CT 文件接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ct-files/upload` | 上传 CT 文件并登记 | PATIENT / DOCTOR | `file_record` `ct_image_file` |
| GET | `/api/ct-files/{id}` | 查询 CT 文件详情 | PATIENT / DOCTOR | `ct_image_file` |
| GET | `/api/ct-files` | 分页查询 CT 文件 | DOCTOR / ADMIN | `ct_image_file` |

**上传 CT 请求参数建议：**

- `patientId`
- `registrationId`
- `medicalRecordId`
- `uploadSource`
- `file`

---

## 11.3 CT 分析任务接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ct-analysis/tasks` | 创建 CT 分析任务 | PATIENT / DOCTOR | `ct_analysis_task` |
| GET | `/api/ct-analysis/tasks/{id}` | 查询任务详情 | PATIENT / DOCTOR | `ct_analysis_task` |
| GET | `/api/ct-analysis/tasks` | 分页查询任务 | DOCTOR / ADMIN | `ct_analysis_task` |
| POST | `/api/ct-analysis/tasks/{id}/retry` | 重试分析任务 | DOCTOR / ADMIN | `ct_analysis_task` |

**创建任务请求体建议：**

```json
{
  "ctImageFileId": 90001,
  "analysisType": "ICH_DETECTION"
}
```

---

## 11.4 CT 分析结果接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/ct-analysis/results/{taskId}` | 查询分析结果 | PATIENT / DOCTOR | `ct_analysis_result` |
| POST | `/api/ct-analysis/results/{taskId}/confirm` | 医生确认分析结果 | DOCTOR | `ct_analysis_result` |
| POST | `/api/ct-analysis/results/{taskId}/reject` | 医生驳回分析结果 | DOCTOR | `ct_analysis_result` |

**结果响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "hasHemorrhage": true,
    "confidence": 0.91,
    "riskLevel": "HIGH",
    "abnormalSlices": [12, 13, 14],
    "heatmapFileId": 91001,
    "modelName": "hemorrhage_resnet50_v1",
    "doctorConfirmStatus": "UNCONFIRMED"
  },
  "timestamp": "2026-06-11T10:00:00Z"
}
```

---

## 12. AI 接口

## 12.1 AI 会话接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ai/chat/sessions` | 创建 AI 会话 | PATIENT / DOCTOR | `ai_chat_session` |
| GET | `/api/ai/chat/sessions/{id}` | 查询会话详情 | PATIENT / DOCTOR | `ai_chat_session` |
| GET | `/api/ai/chat/sessions` | 查询会话列表 | PATIENT / DOCTOR / ADMIN | `ai_chat_session` |
| POST | `/api/ai/chat/sessions/{id}/end` | 结束会话 | PATIENT / DOCTOR | `ai_chat_session` |

**创建会话请求体建议：**

```json
{
  "patientId": 20001,
  "registrationId": 50001,
  "sessionType": "INQUIRY"
}
```

---

## 12.2 AI 消息接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ai/chat/sessions/{id}/messages` | 发送一轮消息并获取回复 | PATIENT / DOCTOR | `ai_chat_message` |
| GET | `/api/ai/chat/sessions/{id}/messages` | 查询会话消息历史 | PATIENT / DOCTOR | `ai_chat_message` |
| GET | `/api/ai/chat/sessions/{id}/stream` | SSE 流式问诊 | PATIENT / DOCTOR | `ai_chat_message` |

**特别说明：**
- 对于 `/stream` SSE 接口，请使用 `GET /api/ai/chat/sessions/{id}/stream?accessToken={token}` 方式鉴权，以兼容浏览器 `EventSource` 限制。

**消息请求体建议：**

```json
{
  "content": "我头痛三天了，还伴有恶心",
  "role": "USER"
}
```

---

## 12.3 AI 导诊接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ai/triage` | 触发 AI 导诊 | PATIENT | `ai_triage_result` |
| GET | `/api/ai/triage/{sessionId}` | 查询导诊结果 | PATIENT / DOCTOR | `ai_triage_result` |

---

## 12.4 AI 病历草稿接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ai/record-drafts` | 生成病历草稿 | DOCTOR | `ai_medical_record_draft` |
| GET | `/api/ai/record-drafts/{id}` | 查询病历草稿 | DOCTOR | `ai_medical_record_draft` |
| POST | `/api/ai/record-drafts/{id}/adopt` | 采用草稿到病历编辑页 | DOCTOR | `ai_medical_record_draft` |
| POST | `/api/ai/record-drafts/{id}/discard` | 丢弃草稿 | DOCTOR | `ai_medical_record_draft` |

---

## 12.5 AI 辅助诊断接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/ai/diagnosis-advice` | 生成辅助诊断建议 | DOCTOR | `ai_diagnosis_advice` |
| GET | `/api/ai/diagnosis-advice/{id}` | 查询诊断建议 | DOCTOR | `ai_diagnosis_advice` |

---

## 12.6 AI 日志接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| GET | `/api/ai/logs` | 查询 AI 调用日志 | ADMIN | `ai_call_log` |
| GET | `/api/ai/logs/{id}` | 查询单条 AI 调用日志 | ADMIN | `ai_call_log` |

---

## 13. 知识库接口

| 方法 | 路径 | 说明 | 角色 | 关联表 |
|---|---|---|---|---|
| POST | `/api/knowledge/documents` | 上传知识文档 | ADMIN | `knowledge_document` `file_record` |
| GET | `/api/knowledge/documents` | 查询知识文档列表 | ADMIN | `knowledge_document` |
| POST | `/api/knowledge/documents/{id}/index` | 文档切分并向量化 | ADMIN | `knowledge_chunk` `knowledge_vector` |
| GET | `/api/knowledge/documents/{id}/chunks` | 查询文档分片 | ADMIN | `knowledge_chunk` |
| GET | `/api/knowledge/search` | 测试知识检索 | ADMIN / DOCTOR | `knowledge_vector` |

---

## 14. 关键接口与数据库关系

### 14.1 登录链路

```text
/api/auth/login
  -> sys_user
  -> sys_user_role
  -> sys_role
  -> doctor / patient
```

### 14.2 挂号链路

```text
/api/registrations
  -> doctor_schedule
  -> registration
  -> visit_queue
  -> payment_order / payment_item
```

### 14.3 医生接诊链路

```text
/api/outpatient/records
  -> medical_record
  -> medical_diagnosis
  -> check_request / inspection_request / prescription
```

### 14.4 AI 问诊链路

```text
/api/ai/chat/sessions
/api/ai/chat/sessions/{id}/messages
  -> ai_chat_session
  -> ai_chat_message
  -> ai_call_log
  -> knowledge_document / knowledge_chunk / knowledge_vector
```

### 14.5 CT 影像链路

```text
/api/ct-files/upload
  -> file_record
  -> ct_image_file

/api/ct-analysis/tasks
  -> ct_analysis_task
  -> Kafka
  -> Python 服务

/api/ct-analysis/results/{taskId}
  -> ct_analysis_result
```

---

## 15. 开发顺序建议

### 第一阶段：必须先做

```text
auth
department
doctor
patient
schedules
registrations
medical_record
prescription
payment_order
```

### 第二阶段：补足门诊闭环

```text
check_request
inspection_request
check_result
inspection_result
dispense
refund
```

### 第三阶段：AI 与影像增强

```text
ai chat
ai triage
ai record draft
knowledge index
ct upload
ct analysis task
ct analysis result confirm
```

---

## 16. 当前结论

这份接口文档的定位是：

- 让后端开发知道每个模块应该暴露哪些核心接口
- 让前端开发知道主要路径、入参和出参大概长什么样
- 让数据库设计和接口设计保持一致

如果下一步继续细化，我建议按下面顺序拆分：

1. 先把 `auth + 挂号 + 病历 + 处方 + 缴费` 做成第一批 Swagger 真实接口
2. 再补 `检查检验 + 药房`
3. 最后再做 `AI + 影像 + RAG`

这样实现节奏会和数据库落表节奏保持一致，不容易乱。
