# CT 模块使用说明

## 1. 模块功能

当前 CT 模块用于完成 `B1/B2` 头部 CT 分类分析，后端能力包括：

- 创建 CT 分析任务
- 异步调用 Python 推理脚本
- 自动适配本地文件或 MinIO 对象
- 保存分析任务状态与分析结果
- 提供结果查询接口

当前仅支持一种分析类型：

- `B1_B2_CLASSIFICATION`


## 2. 接口说明

### 2.1 创建 CT 分析任务

接口：

```text
POST /api/ct-analysis/tasks
```

请求体示例：

```json
{
  "ctImageFileId": 90001,
  "analysisType": "B1_B2_CLASSIFICATION"
}
```

字段说明：

- `ctImageFileId`：`file_record` 表中的 CT 文件记录 ID
- `analysisType`：当前固定为 `B1_B2_CLASSIFICATION`

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "ctImageFileId": 90001,
    "analysisType": "B1_B2_CLASSIFICATION",
    "taskStatus": "PENDING",
    "submittedAt": "2026-06-16T14:30:00"
  },
  "timestamp": "2026-06-16T14:30:00"
}
```


### 2.2 查询 CT 分析结果

接口：

```text
GET /api/ct-analysis/results/{taskId}
```

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "analysisType": "B1_B2_CLASSIFICATION",
    "taskStatus": "SUCCESS",
    "predictedCategory": "B2",
    "confidence": 0.912345,
    "classProbabilities": {
      "B1": 0.087655,
      "B2": 0.912345
    },
    "riskLevel": "HIGH",
    "modelName": "cq500_b1_b2_classifier_v1",
    "doctorConfirmStatus": "UNCONFIRMED",
    "failureReason": null
  },
  "timestamp": "2026-06-16T14:31:00"
}
```


## 3. 任务执行流程

调用 `POST /api/ct-analysis/tasks` 后，系统执行流程如下：

1. 根据 `ctImageFileId` 查询 `file_record`
2. 判断 `objectKey` 是否为本地可读路径
3. 如果不是本地路径，则根据 `bucketName + objectKey` 从 MinIO 下载到本地缓存目录
4. 创建 `ct_analysis_task` 任务记录
5. 异步调用 Python 脚本进行推理
6. 将推理结果写入 `ct_analysis_result`
7. 前端通过 `GET /api/ct-analysis/results/{taskId}` 查询任务执行状态和结果


## 4. 任务状态说明

任务状态字段为 `taskStatus`，当前可能值如下：

- `PENDING`：任务已创建，等待执行
- `RUNNING`：推理执行中
- `SUCCESS`：推理成功，结果已保存
- `FAILED`：推理失败

如果任务失败，可通过结果接口中的 `failureReason` 查看失败原因。


## 5. 输入文件适配规则

当前模块支持两种输入来源：

### 5.1 本地路径

如果 `file_record.object_key` 本身就是本地存在的目录或文件路径，则直接作为 Python 推理脚本输入。

### 5.2 MinIO 对象

如果 `file_record.object_key` 不是本地路径，则系统会按以下逻辑处理：

- 使用 `file_record.bucket_name` 和 `file_record.object_key`
- 从 MinIO 下载到本地缓存目录
- 下载完成后，把本地缓存目录或本地缓存文件路径交给 Python 推理脚本

支持两种 MinIO 存储形式：

- 一个病例目录前缀下存放多张 DICOM 文件
- 一个单独对象直接作为推理输入

注意：

- 如果实际上传的是 `.zip` 压缩包，当前版本还没有自动解压逻辑
- 如果后续使用压缩包上传，需要继续扩展“下载后自动解压再推理”


## 6. 数据库表说明

当前模块新增两张表：

### 6.1 `ct_analysis_task`

用于保存 CT 分析任务信息，包括：

- 关联的 CT 文件 ID
- 分析类型
- 任务状态
- 本地推理输入路径
- 失败原因
- 提交、开始、结束时间

### 6.2 `ct_analysis_result`

用于保存 CT 分析结果，包括：

- 任务 ID
- 预测类别
- 置信度
- `B1/B2` 两类概率
- 风险等级
- 模型名称
- 医生确认状态
- 原始推理结果 JSON

建表 SQL 位置：

- `src/main/resources/db/schema/phase1-minimal-business.sql`


## 7. 配置说明

配置文件位置：

- `src/main/resources/application.yml`

### 7.1 CT 推理配置

```yaml
app:
  ct-analysis:
    python-executable: /opt/anaconda3/envs/neuedu/bin/python
    script-path: .md/ct/infer_b1_b2.py
    model-path: .md/ct/best_b1_b2_model.pth
    timeout-seconds: 300
    local-cache-dir: datas/ct-analysis-cache
```

字段说明：

- `python-executable`：Python 可执行文件路径
- `script-path`：CT 推理脚本路径
- `model-path`：模型权重文件路径
- `timeout-seconds`：单次推理超时时间，单位秒
- `local-cache-dir`：MinIO 下载后的本地缓存目录

### 7.2 MinIO 配置

```yaml
minio:
  endpoint: http://localhost:9005
  access-key: minioadmin
  secret-key: minioadmin
```

字段说明：

- `endpoint`：MinIO 服务地址
- `access-key`：访问密钥
- `secret-key`：访问密码


## 8. 使用前准备

在调用 CT 模块前，需要确保以下内容正确：

### 8.1 文件记录已存在

`file_record` 表中需要有对应的 CT 文件记录，至少包含以下字段：

- `id`
- `bucket_name`
- `object_key`
- `original_name`

### 8.2 Python 推理环境可用

需要保证以下内容可用：

- Python 环境存在
- 推理脚本可执行
- 模型权重文件路径正确
- Python 依赖已安装完成

### 8.3 MinIO 可访问

如果输入文件存储在 MinIO 中，需要保证：

- MinIO 服务可连通
- `bucket_name` 正确
- `object_key` 正确
- 当前账号有读取权限


## 9. 代码位置

主要代码如下：

- 控制器：`src/main/java/com/neusoft/neu23/neuhospital/ct/controller/CtAnalysisController.java`
- 任务服务：`src/main/java/com/neusoft/neu23/neuhospital/ct/service/impl/CtAnalysisServiceImpl.java`
- 输入适配服务：`src/main/java/com/neusoft/neu23/neuhospital/ct/service/CtInputResolverService.java`
- Python 调用服务：`src/main/java/com/neusoft/neu23/neuhospital/ct/service/CtAiInferenceService.java`
- 异步执行服务：`src/main/java/com/neusoft/neu23/neuhospital/ct/service/CtAnalysisAsyncService.java`
- 配置类：`src/main/java/com/neusoft/neu23/neuhospital/ct/config/`


## 10. 当前限制

当前版本限制如下：

- 仅支持 `B1_B2_CLASSIFICATION`
- 不支持热力图输出
- 不支持异常切片定位
- 不支持自动解压 `.zip` 压缩包
- 当前返回的是病例级分类结果，不是逐切片分析结果


## 11. 后续可扩展方向

后续如果继续增强 CT 模块，可以扩展：

- 支持 `.zip` 自动解压
- 支持更多 CT 分类模型
- 支持出血检测模型
- 支持异常切片定位
- 支持热力图可视化输出
- 支持医生确认结果回写
- 支持任务重试和缓存清理
