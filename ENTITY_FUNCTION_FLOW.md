# 项目实体功能与业务流程清单

本文档根据当前项目代码整理，主要参考 `entity`、`controller`、`service/impl`、`mapper` 和数据库初始化脚本。

## 一、已实现实体功能清单

### 1. 认证与系统账号

#### SysUserEntity（系统用户）

- 相关表：`sys_user`
- 主要用途：登录账号载体，关联患者或医生业务身份。
- 已实现功能：
  - 用户登录校验。
  - 注册患者账号时写入系统用户。
  - 新增患者时同步创建系统用户。
  - 新增医生时同步创建系统用户。
  - 获取当前登录用户信息。
- 主要入口：
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET /api/auth/me`

#### SysRoleEntity（系统角色）

- 相关表：`sys_role`
- 已实现情况：
  - 已有实体类和 Mapper。
  - 当前未看到独立的角色管理接口。

#### SysUserRoleEntity（用户角色关系）

- 相关表：`sys_user_role`
- 已实现情况：
  - 已有实体类和 Mapper。
  - 当前未看到独立的用户角色分配接口。

## 2. 患者、医生、科室

#### PatientEntity（患者）

- 相关表：`patient`
- 已实现功能：
  - 新增患者。
  - 按 ID 查询患者。
  - 更新患者资料。
  - 分页查询患者，支持关键词搜索。
  - 创建患者时同步创建 `sys_user` 登录账号。
- 主要入口：
  - `POST /api/patients`
  - `GET /api/patients/{id}`
  - `PUT /api/patients/{id}`
  - `GET /api/patients`

#### DepartmentEntity（科室）

- 相关表：`department`
- 已实现功能：
  - 新增科室。
  - 按 ID 查询科室。
  - 更新科室。
  - 查询全部科室。
  - 查询科室树。
- 主要入口：
  - `POST /api/departments`
  - `GET /api/departments/{id}`
  - `PUT /api/departments/{id}`
  - `GET /api/departments`
  - `GET /api/departments/tree`

#### DoctorEntity（医生）

- 相关表：`doctor`
- 已实现功能：
  - 新增医生。
  - 按 ID 查询医生。
  - 更新医生资料。
  - 分页查询医生，支持科室和关键词筛选。
  - 按科室查询医生列表。
  - 创建医生时同步创建 `sys_user` 登录账号。
- 主要入口：
  - `POST /api/doctors`
  - `GET /api/doctors/{id}`
  - `PUT /api/doctors/{id}`
  - `GET /api/doctors/page`
  - `GET /api/doctors`

## 3. 排班、挂号、队列

#### DoctorScheduleEntity（医生排班）

- 相关表：`doctor_schedule`
- 已实现功能：
  - 创建医生排班。
  - 更新医生排班。
  - 关闭排班。
  - 分页查询排班，支持医生、科室、日期、时段筛选。
  - 维护号源数量：`sourceCount`、`availableCount`。
- 主要入口：
  - `POST /api/schedules`
  - `PUT /api/schedules/{id}`
  - `POST /api/schedules/{id}/close`
  - `GET /api/schedules`

#### RegistrationEntity（挂号单）

- 相关表：`registration`
- 已实现功能：
  - 快速挂号。
  - 查询我的挂号。
  - 查询全部挂号。
  - 查询挂号详情。
  - 取消挂号。
  - 当日签到。
  - 签到后创建就诊队列记录。
  - 支付成功后由事件监听修改挂号状态。
  - 支付超时后由事件监听回滚挂号状态。
- 并发与一致性能力：
  - Redis Lua 限流。
  - Redisson 分布式锁防重复提交。
  - Redis 预扣号源库存。
  - 本地消息表记录挂号消息。
  - Kafka 消费后执行真实挂号落库。
  - 事务回滚时补偿 Redis 库存。
- 主要入口：
  - `POST /api/registrations/quick`
  - `GET /api/registrations/my`
  - `GET /api/registrations`
  - `GET /api/registrations/{id}`
  - `POST /api/registrations/{id}/cancel`
  - `POST /api/registrations/{id}/check-in`

#### RegistrationMessageLogEntity（挂号消息日志）

- 相关表：`registration_message_log`
- 已实现功能：
  - 快速挂号成功预扣库存后写入本地消息表。
  - 定时任务扫描待发送消息。
  - Kafka 发送挂号消息。
  - Kafka 消费者执行挂号落库逻辑。
- 主要用途：
  - 支撑挂号高并发场景下的最终一致性。

#### VisitQueueEntity（就诊队列）

- 相关表：`visit_queue`
- 已实现功能：
  - 医生查看自己的候诊队列。
  - 叫号。
  - 跳过患者。
  - 完成就诊。
  - 完成就诊时同步更新挂号单状态。
- 主要入口：
  - `GET /api/queue/doctor/me`
  - `POST /api/queue/{id}/call`
  - `POST /api/queue/{id}/skip`
  - `POST /api/queue/{id}/finish`

## 4. 门诊病历与诊断

#### MedicalRecordEntity（门诊病历）

- 相关表：`medical_record`
- 已实现功能：
  - 创建病历。
  - 查询病历详情。
  - 更新病历。
  - 确认病历。
  - 分页查询病历，支持患者和医生筛选。
- 主要入口：
  - `POST /api/outpatient/records`
  - `GET /api/outpatient/records/{id}`
  - `PUT /api/outpatient/records/{id}`
  - `POST /api/outpatient/records/{id}/confirm`
  - `GET /api/outpatient/records`

#### MedicalDiagnosisEntity（诊断）

- 相关表：`medical_diagnosis`
- 已实现功能：
  - 为指定病历保存诊断列表。
  - 查询指定病历的诊断列表。
- 主要入口：
  - `POST /api/outpatient/records/{id}/diagnoses`
  - `GET /api/outpatient/records/{id}/diagnoses`

## 5. 检查、检验、处置

#### CheckRequestEntity（检查申请）

- 相关表：`check_request`
- 已实现功能：
  - 创建检查申请。
  - 查询检查申请详情。
  - 分页查询检查申请，支持患者和医生筛选。
  - 取消检查申请。
  - 支付成功后更新状态。
  - 支付超时后回滚状态。
- 主要入口：
  - `POST /api/check-requests`
  - `GET /api/check-requests/{id}`
  - `GET /api/check-requests`
  - `POST /api/check-requests/{id}/cancel`

#### CheckResultEntity（检查结果）

- 相关表：`check_result`
- 已实现功能：
  - 录入检查结果。
  - 查询检查结果详情。
  - 确认检查结果。
  - 录入或确认结果后回写检查申请状态和摘要。
- 主要入口：
  - `POST /api/check-results`
  - `GET /api/check-results/{id}`
  - `POST /api/check-results/{id}/confirm`

#### InspectionRequestEntity（检验申请）

- 相关表：`inspection_request`
- 已实现功能：
  - 创建检验申请。
  - 查询检验申请详情。
  - 分页查询检验申请，支持患者和医生筛选。
  - 取消检验申请。
  - 支付成功后更新状态。
  - 支付超时后回滚状态。
- 主要入口：
  - `POST /api/inspection-requests`
  - `GET /api/inspection-requests/{id}`
  - `GET /api/inspection-requests`
  - `POST /api/inspection-requests/{id}/cancel`

#### InspectionResultEntity（检验结果）

- 相关表：`inspection_result`
- 已实现功能：
  - 录入检验结果。
  - 查询检验结果详情。
  - 确认检验结果。
  - 录入结果后将检验申请改为执行中并回写摘要。
  - 确认结果后将检验申请改为已出报告。
- 主要入口：
  - `POST /api/inspection-results`
  - `GET /api/inspection-results/{id}`
  - `POST /api/inspection-results/{id}/confirm`

#### InspectionResultItemEntity（检验结果明细）

- 相关表：`inspection_result_item`
- 已实现功能：
  - 作为检验结果的子项保存。
  - 录入检验结果时批量保存项目明细。

#### DisposalRequestEntity（处置申请）

- 相关表：`disposal_request`
- 已实现功能：
  - 创建处置申请。
  - 查询处置申请详情。
  - 分页查询处置申请，支持患者和医生筛选。
  - 取消处置申请。
  - 完成处置申请。
- 主要入口：
  - `POST /api/disposal-requests`
  - `GET /api/disposal-requests/{id}`
  - `GET /api/disposal-requests`
  - `POST /api/disposal-requests/{id}/cancel`
  - `POST /api/disposal-requests/{id}/finish`

## 6. 药房、处方、发药

#### DrugInfoEntity（药品）

- 相关表：`drug_info`
- 已实现功能：
  - 新增药品。
  - 更新药品。
  - 调整库存。
  - 查询药品详情。
  - 分页查询药品，支持关键词和分类筛选。
  - 发药时按处方明细原子扣减库存。
- 主要入口：
  - `POST /api/drugs`
  - `PUT /api/drugs/{id}`
  - `POST /api/drugs/{id}/stock-adjust`
  - `GET /api/drugs/{id}`
  - `GET /api/drugs`

#### PrescriptionEntity（处方）

- 相关表：`prescription`
- 已实现功能：
  - 创建处方。
  - 查询处方详情。
  - 分页查询处方，支持患者和医生筛选。
  - 支付成功后由事件监听修改处方状态。
  - 支付超时后由事件监听回滚处方状态。
  - 发药后修改为已发药状态。
- 主要入口：
  - `POST /api/prescriptions`
  - `GET /api/prescriptions/{id}`
  - `GET /api/prescriptions`

#### PrescriptionItemEntity（处方明细）

- 相关表：`prescription_item`
- 已实现功能：
  - 创建处方时保存药品明细。
  - 根据药品价格、数量计算明细金额。
  - 发药时作为库存扣减依据。

#### DrugDispenseRecordEntity（发药记录）

- 相关表：`drug_dispense_record`
- 已实现功能：
  - 对已缴费处方执行发药。
  - 校验处方是否存在。
  - 校验处方是否已缴费。
  - 按处方明细扣减药品库存。
  - 修改处方状态为已发药。
  - 保存发药记录。
- 主要入口：
  - `POST /api/pharmacy/dispense`

## 7. 支付

#### PaymentOrderEntity（支付订单）

- 相关表：`payment_order`
- 已实现功能：
  - 查询患者待缴费项目。
  - 创建支付订单。
  - 模拟支付成功。
  - 定时扫描超过 15 分钟未支付订单并取消。
  - 支付成功后发布业务事件。
  - 支付超时后发布业务事件。
- 待缴费来源：
  - 未缴费挂号单。
  - 新建检查申请。
  - 新建检验申请。
  - 新建处方。
- 主要入口：
  - `GET /api/payment/pending`
  - `POST /api/payment/create`
  - `POST /api/payment/{id}/pay`

#### PaymentItemEntity（支付明细）

- 相关表：`payment_item`
- 已实现功能：
  - 创建支付订单时生成明细。
  - 支付成功后明细状态改为已支付。
  - 支付超时后明细状态改为已取消。

#### RefundRecordEntity（退款记录）

- 相关表：`refund_record`
- 已实现情况：
  - 已有实体类、表结构和 Mapper。
  - 当前未看到独立退款接口。

## 8. 文件

#### FileRecordEntity（文件记录）

- 相关表：`file_record`
- 已实现功能：
  - 单文件上传。
  - 目录上传。
  - 文件分页查询。
  - 查询文件详情。
  - 生成预览 URL。
  - 下载文件。
  - 预览文件。
- 主要入口：
  - `POST /api/files/upload`
  - `POST /api/files/upload-directory`
  - `GET /api/files`
  - `GET /api/files/{fileId}`
  - `GET /api/files/{fileId}/preview-url`
  - `GET /api/files/{fileId}/download`
  - `GET /api/files/{fileId}/preview`

## 9. AI 聊天

#### AiChatSessionEntity（AI 会话）

- 相关表：`ai_chat_session`
- 已实现功能：
  - 创建 AI 聊天会话。
  - 创建会话时校验患者与挂号记录归属关系。
  - 会话状态控制。
- 主要入口：
  - `POST /api/ai/chat/sessions`

#### AiChatMessageEntity（AI 消息）

- 相关表：`ai_chat_message`
- 已实现功能：
  - 保存用户消息。
  - 保存 AI 回复。
  - 按会话加载历史消息。
  - 组装系统提示词和历史上下文。
  - 调用 Spring AI ChatClient。
  - 支持工具函数：查询患者信息、更新患者记忆、查询科室、查询排班、预约挂号。
- 主要入口：
  - `POST /api/ai/chat/sessions/{sessionNo}/messages`

## 10. CT 分析

#### CtAnalysisTaskEntity（CT 分析任务）

- 相关表：`ct_analysis_task`
- 已实现功能：
  - 创建 CT 分析任务。
  - 校验 CT 文件记录是否存在。
  - 记录任务状态。
  - 异步执行分析任务。
  - 任务执行中、完成、失败状态更新。
- 主要入口：
  - `POST /api/ct-analysis/tasks`

#### CtAnalysisResultEntity（CT 分析结果）

- 相关表：`ct_analysis_result`
- 已实现功能：
  - 保存 CT AI 推理结果。
  - 更新已有任务结果。
  - 查询指定任务的分析结果。
  - 记录预测分类、置信度、风险等级、模型名称和原始 JSON。
- 主要入口：
  - `GET /api/ct-analysis/results/{taskId}`

## 二、主要业务流程使用顺序

### 流程 1：基础数据准备流程

1. 创建科室：`POST /api/departments`
2. 创建医生：`POST /api/doctors`
3. 创建患者：`POST /api/patients`
4. 创建医生排班：`POST /api/schedules`
5. 查询排班：`GET /api/schedules`

涉及实体顺序：

1. `DepartmentEntity`
2. `DoctorEntity`
3. `SysUserEntity`
4. `PatientEntity`
5. `DoctorScheduleEntity`

### 流程 2：患者挂号流程

1. 患者查询科室：`GET /api/departments`
2. 患者查询医生：`GET /api/doctors`
3. 患者查询排班：`GET /api/schedules`
4. 患者快速挂号：`POST /api/registrations/quick`
5. 系统执行 Redis 限流。
6. 系统执行 Redisson 防重复提交。
7. 系统预扣 Redis 号源库存。
8. 系统写入 `RegistrationMessageLogEntity`。
9. 定时任务发送 Kafka 挂号消息。
10. Kafka 消费者扣减数据库排班库存。
11. Kafka 消费者创建 `RegistrationEntity` 挂号单，状态为待缴费。
12. 患者查询我的挂号：`GET /api/registrations/my`

涉及实体顺序：

1. `DepartmentEntity`
2. `DoctorEntity`
3. `DoctorScheduleEntity`
4. `RegistrationMessageLogEntity`
5. `RegistrationEntity`

### 流程 3：挂号缴费与签到入队流程

1. 患者查询待缴费项目：`GET /api/payment/pending?patientId={patientId}`
2. 创建支付订单：`POST /api/payment/create`
3. 系统创建 `PaymentOrderEntity`。
4. 系统创建 `PaymentItemEntity`。
5. 模拟支付成功：`POST /api/payment/{id}/pay`
6. 系统修改支付订单和支付明细为已支付。
7. 系统发布支付成功事件。
8. 挂号监听器接收事件，将挂号单状态改为已支付。
9. 就诊当日患者签到：`POST /api/registrations/{id}/check-in`
10. 系统修改挂号单状态为就诊中。
11. 系统创建 `VisitQueueEntity` 队列记录。

涉及实体顺序：

1. `RegistrationEntity`
2. `PaymentOrderEntity`
3. `PaymentItemEntity`
4. `VisitQueueEntity`

### 流程 4：医生接诊队列流程

1. 医生查询自己的候诊队列：`GET /api/queue/doctor/me`
2. 医生叫号：`POST /api/queue/{id}/call`
3. 如患者未到，医生跳过：`POST /api/queue/{id}/skip`
4. 医生完成接诊：`POST /api/queue/{id}/finish`
5. 系统修改队列状态为已完成。
6. 系统同步修改挂号单状态为已完成。

涉及实体顺序：

1. `VisitQueueEntity`
2. `RegistrationEntity`

### 流程 5：门诊病历与诊断流程

1. 医生创建门诊病历：`POST /api/outpatient/records`
2. 医生更新病历内容：`PUT /api/outpatient/records/{id}`
3. 医生保存诊断列表：`POST /api/outpatient/records/{id}/diagnoses`
4. 医生查询诊断列表：`GET /api/outpatient/records/{id}/diagnoses`
5. 医生确认病历：`POST /api/outpatient/records/{id}/confirm`
6. 医生或患者查询病历详情：`GET /api/outpatient/records/{id}`

涉及实体顺序：

1. `RegistrationEntity`
2. `MedicalRecordEntity`
3. `MedicalDiagnosisEntity`

### 流程 6：检查申请、缴费、结果流程

1. 医生创建检查申请：`POST /api/check-requests`
2. 患者查询待缴费项目：`GET /api/payment/pending?patientId={patientId}`
3. 患者创建支付订单：`POST /api/payment/create`
4. 患者支付：`POST /api/payment/{id}/pay`
5. 系统发布支付成功事件。
6. 检查监听器更新检查申请状态。
7. 医生或检查科室录入检查结果：`POST /api/check-results`
8. 查询检查结果详情：`GET /api/check-results/{id}`
9. 确认检查结果：`POST /api/check-results/{id}/confirm`
10. 系统回写检查申请的结果摘要和状态。

涉及实体顺序：

1. `CheckRequestEntity`
2. `PaymentOrderEntity`
3. `PaymentItemEntity`
4. `CheckResultEntity`

### 流程 7：检验申请、缴费、结果流程

1. 医生创建检验申请：`POST /api/inspection-requests`
2. 患者查询待缴费项目：`GET /api/payment/pending?patientId={patientId}`
3. 患者创建支付订单：`POST /api/payment/create`
4. 患者支付：`POST /api/payment/{id}/pay`
5. 系统发布支付成功事件。
6. 检验监听器更新检验申请状态。
7. 医生或检验科室录入检验结果：`POST /api/inspection-results`
8. 系统保存 `InspectionResultEntity`。
9. 系统保存多个 `InspectionResultItemEntity`。
10. 系统将检验申请状态改为执行中。
11. 查询检验结果详情：`GET /api/inspection-results/{id}`
12. 确认检验结果：`POST /api/inspection-results/{id}/confirm`
13. 系统将检验结果改为已确认。
14. 系统将检验申请改为已出报告。

涉及实体顺序：

1. `InspectionRequestEntity`
2. `PaymentOrderEntity`
3. `PaymentItemEntity`
4. `InspectionResultEntity`
5. `InspectionResultItemEntity`

### 流程 8：处置申请流程

1. 医生创建处置申请：`POST /api/disposal-requests`
2. 系统保存 `DisposalRequestEntity`，状态为新建。
3. 查询处置申请详情：`GET /api/disposal-requests/{id}`
4. 分页查询处置申请：`GET /api/disposal-requests`
5. 如不再执行，取消处置申请：`POST /api/disposal-requests/{id}/cancel`
6. 如已执行完成，完成处置申请：`POST /api/disposal-requests/{id}/finish`

涉及实体顺序：

1. `DisposalRequestEntity`

### 流程 9：开处方、缴费、发药流程

1. 医生创建处方：`POST /api/prescriptions`
2. 系统保存 `PrescriptionEntity`。
3. 系统保存多个 `PrescriptionItemEntity`。
4. 患者查询待缴费项目：`GET /api/payment/pending?patientId={patientId}`
5. 患者创建支付订单：`POST /api/payment/create`
6. 患者支付：`POST /api/payment/{id}/pay`
7. 系统发布支付成功事件。
8. 药房监听器更新处方状态为已支付。
9. 药房发药：`POST /api/pharmacy/dispense`
10. 系统校验处方已缴费。
11. 系统根据处方明细扣减 `DrugInfoEntity` 药品库存。
12. 系统修改处方状态为已发药。
13. 系统保存 `DrugDispenseRecordEntity` 发药记录。

涉及实体顺序：

1. `PrescriptionEntity`
2. `PrescriptionItemEntity`
3. `PaymentOrderEntity`
4. `PaymentItemEntity`
5. `DrugInfoEntity`
6. `DrugDispenseRecordEntity`

### 流程 10：药品管理流程

1. 新增药品：`POST /api/drugs`
2. 查询药品：`GET /api/drugs`
3. 查询药品详情：`GET /api/drugs/{id}`
4. 更新药品信息：`PUT /api/drugs/{id}`
5. 调整药品库存：`POST /api/drugs/{id}/stock-adjust`

涉及实体顺序：

1. `DrugInfoEntity`

### 流程 11：文件上传与业务关联流程

1. 上传单个文件：`POST /api/files/upload`
2. 或上传目录：`POST /api/files/upload-directory`
3. 系统保存 `FileRecordEntity`。
4. 按业务类型和业务 ID 查询文件：`GET /api/files`
5. 查询文件详情：`GET /api/files/{fileId}`
6. 获取预览地址：`GET /api/files/{fileId}/preview-url`
7. 下载文件：`GET /api/files/{fileId}/download`
8. 预览文件：`GET /api/files/{fileId}/preview`

涉及实体顺序：

1. `FileRecordEntity`

### 流程 12：AI 问诊聊天流程

1. 创建 AI 会话：`POST /api/ai/chat/sessions`
2. 系统校验患者与挂号记录归属。
3. 系统保存 `AiChatSessionEntity`。
4. 患者发送消息：`POST /api/ai/chat/sessions/{sessionNo}/messages`
5. 系统保存用户消息 `AiChatMessageEntity`。
6. 系统加载当前会话历史消息。
7. 系统组装系统提示词。
8. 系统调用大模型。
9. 系统保存 AI 回复 `AiChatMessageEntity`。
10. 返回 AI 回复内容。

涉及实体顺序：

1. `RegistrationEntity`
2. `AiChatSessionEntity`
3. `AiChatMessageEntity`

### 流程 13：AI 工具辅助挂号流程

1. 患者创建 AI 会话：`POST /api/ai/chat/sessions`
2. 患者通过聊天询问科室。
3. AI 工具查询科室信息。
4. 患者询问排班。
5. AI 工具查询排班信息。
6. 患者确认挂号。
7. AI 工具调用挂号能力。
8. 后续进入普通挂号缴费流程。

涉及实体顺序：

1. `AiChatSessionEntity`
2. `AiChatMessageEntity`
3. `DepartmentEntity`
4. `DoctorScheduleEntity`
5. `RegistrationMessageLogEntity`
6. `RegistrationEntity`

### 流程 14：CT 分析流程

1. 上传 CT 文件：`POST /api/files/upload`
2. 系统保存 `FileRecordEntity`。
3. 创建 CT 分析任务：`POST /api/ct-analysis/tasks`
4. 系统校验 CT 文件记录。
5. 系统保存 `CtAnalysisTaskEntity`，任务状态为待执行。
6. 系统异步执行 CT 分析。
7. 系统解析输入文件到本地路径。
8. 系统调用 AI 推理服务。
9. 系统保存或更新 `CtAnalysisResultEntity`。
10. 系统更新任务状态为完成或失败。
11. 查询分析结果：`GET /api/ct-analysis/results/{taskId}`

涉及实体顺序：

1. `FileRecordEntity`
2. `CtAnalysisTaskEntity`
3. `CtAnalysisResultEntity`

### 流程 15：支付超时取消流程

1. 患者创建支付订单。
2. 系统保存 `PaymentOrderEntity`，状态为未支付。
3. 系统保存 `PaymentItemEntity`，状态为未支付。
4. 定时任务每分钟扫描超过 15 分钟未支付的订单。
5. 系统将支付订单状态改为已取消。
6. 系统将支付明细状态改为已取消。
7. 系统发布支付超时事件。
8. 对应业务监听器回滚业务状态。

涉及实体顺序：

1. `PaymentOrderEntity`
2. `PaymentItemEntity`
3. `RegistrationEntity`
4. `CheckRequestEntity`
5. `InspectionRequestEntity`
6. `PrescriptionEntity`

### 流程 16：退号流程

1. 患者查询我的挂号：`GET /api/registrations/my`
2. 患者取消挂号：`POST /api/registrations/{id}/cancel`
3. 系统校验挂号单归属。
4. 系统校验当前状态是否允许退号。
5. 系统修改挂号单状态为已取消。
6. 系统回滚数据库排班可用号源。
7. 系统回滚 Redis 号源库存。

涉及实体顺序：

1. `RegistrationEntity`
2. `DoctorScheduleEntity`

## 三、当前已有实体但功能相对不完整的部分

1. `RefundRecordEntity`：已有实体、表和 Mapper，但未看到退款接口。
2. `SysRoleEntity`：已有实体和 Mapper，但未看到角色管理接口。
3. `SysUserRoleEntity`：已有实体和 Mapper，但未看到用户角色分配接口。
4. 数据库脚本中存在部分 AI/RAG/知识库表，例如 `knowledge_document`、`knowledge_chunk`、`ai_triage_result` 等，但当前未看到完整 Java 实体和业务接口。
