# CT 模块前端搭建说明

## 1. 目标

CT 模块前端当前的核心目标不是“自己直接对接 MinIO”，而是完成下面这条业务链路：

1. 选择一条已经存在的 `file_record`
2. 取到它的 `ctImageFileId`
3. 调用后端创建 CT 分析任务
4. 轮询任务结果
5. 展示分析状态、分类结果和失败原因

当前后端仅支持：

- `B1_B2_CLASSIFICATION`


## 2. 先说清楚前后端边界

这部分很重要，前端接入时不要误解。

### 2.1 前端当前不直接调用 MinIO

你现在项目里的 CT 流程中，MinIO 是后端内部使用的：

- 前端只提交 `ctImageFileId`
- 后端根据 `ctImageFileId` 去查 `file_record`
- 后端再根据 `bucketName + objectKey` 决定是否从 MinIO 下载
- 下载完成后，把本地缓存路径交给 Python 推理脚本

也就是说：

- 前端不需要自己拼 MinIO 下载地址
- 前端也不需要自己把 MinIO 文件下载到浏览器再上传给 CT 接口

### 2.2 当前项目还没有通用文件上传/下载接口

目前仓库里：

- 有 `file_record` 表和 `FileRecordEntity`
- 有 MinIO 客户端配置
- 有 CT 场景内部下载逻辑

但还没有真正暴露出来的：

- 通用上传接口
- 通用文件列表接口
- 通用下载接口
- 通用预览接口

所以当前前端说明必须按“先使用已有 `file_record` 记录”来设计，不能写成完整的“上传 CT 文件 -> 获取 fileId -> 发起分析”的已实现闭环。


## 3. 当前真实接口

CT 相关目前只有两个对外接口：

### 3.1 创建任务

```text
POST /api/ct-analysis/tasks
```

请求体：

```json
{
  "ctImageFileId": 90001,
  "analysisType": "B1_B2_CLASSIFICATION"
}
```

### 3.2 查询结果

```text
GET /api/ct-analysis/results/{taskId}
```


## 4. `ctImageFileId` 到底是什么

`ctImageFileId` 不是 CT 任务表的 ID，也不是 MinIO 的 objectKey，而是：

- `file_record.id`

前端在发起 CT 分析前，必须先拿到一条可用的 `file_record`。

至少应保证这条 `file_record` 里有这些字段：

- `id`
- `bucket_name`
- `object_key`
- `original_name`

如果后端要从 MinIO 取文件，还应保证：

- `bucket_name` 正确
- `object_key` 正确


## 5. 后端对文件输入的真实处理逻辑

后端创建任务后，会这样处理输入：

1. 根据 `ctImageFileId` 查询 `file_record`
2. 读取 `objectKey`
3. 如果 `objectKey` 本身就是本地存在的路径，则直接把它作为推理输入
4. 如果不是本地路径，则读取 `bucketName + objectKey`
5. 后端从 MinIO 下载到 `local-cache-dir`
6. 再把下载后的本地目录或本地文件路径交给 Python 推理脚本

这意味着前端只需要关心：

- `ctImageFileId` 是否存在
- 当前记录是否确实指向一个可分析的 CT 文件或病例目录

不需要关心：

- MinIO 下载细节
- 本地缓存目录位置
- Python 推理脚本路径


## 6. 页面建议

建议先做一个独立页面，例如：

- `/ct-analysis`

页面建议拆成 4 个区域：

- 文件记录选择区
- 任务提交区
- 任务状态区
- 结果展示区


## 7. 页面结构建议

### 7.1 文件记录选择区

当前最稳的做法是围绕 `ctImageFileId` 来做。

如果你们前端还没有文件管理页，建议先做成简单版：

- 一个 `ctImageFileId` 输入框
- 一个只读提示区，告诉用户这是 `file_record.id`
- 一个说明文案，提示这条记录必须对应 CT 文件

如果后续你补了文件模块接口，可以升级成：

- 文件列表表格
- 搜索 `originalName`
- 按 `bizType` 筛选
- 选中一条记录后自动带出 `ctImageFileId`

### 7.2 任务提交区

建议放一个主按钮：

- `开始分析`

点击后调用：

```text
POST /api/ct-analysis/tasks
```

按钮状态建议：

- 未输入文件 ID 时禁用
- 请求发送中显示 `提交中`
- 提交成功后显示 `已创建任务`

### 7.3 任务状态区

建议单独展示：

- `taskId`
- `taskStatus`
- `submittedAt`

这样即使还没出结果，前端也能明确告诉用户任务已经创建成功。

### 7.4 结果展示区

建议展示以下内容：

- 任务状态
- 预测类别
- 置信度
- `B1` 概率
- `B2` 概率
- 风险等级
- 模型名称
- 医生确认状态
- 失败原因


## 8. 接口对接说明

### 8.1 创建任务接口

```text
POST /api/ct-analysis/tasks
```

示例请求：

```json
{
  "ctImageFileId": 90001,
  "analysisType": "B1_B2_CLASSIFICATION"
}
```

示例响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "ctImageFileId": 90001,
    "analysisType": "B1_B2_CLASSIFICATION",
    "taskStatus": "PENDING",
    "submittedAt": "2026-06-18T10:00:00"
  },
  "timestamp": "2026-06-18T10:00:00"
}
```

前端在这个阶段应保存：

- `taskId`
- `taskStatus`
- `submittedAt`

### 8.2 查询结果接口

```text
GET /api/ct-analysis/results/{taskId}
```

示例响应：

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
  "timestamp": "2026-06-18T10:00:10"
}
```


## 9. 轮询逻辑

前端在创建任务成功后，需要根据 `taskId` 轮询结果接口。

建议规则：

- 轮询间隔：2 秒
- 最大轮询时长：2 到 5 分钟
- 当状态为 `SUCCESS` 时停止轮询
- 当状态为 `FAILED` 时停止轮询

建议状态文案：

- `PENDING`：任务已创建，等待执行
- `RUNNING`：模型分析中
- `SUCCESS`：分析完成
- `FAILED`：分析失败

额外建议：

- 页面卸载时停止轮询
- 用户重新发起任务时，先停止上一轮轮询
- 连续多次请求失败时提示“结果查询失败，请稍后重试”


## 10. 前端数据结构建议

### 10.1 创建任务返回

```ts
type CtAnalysisTask = {
  taskId: number
  ctImageFileId: number
  analysisType: string
  taskStatus: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  submittedAt: string
}
```

### 10.2 分析结果返回

```ts
type CtAnalysisResult = {
  taskId: number
  analysisType: string
  taskStatus: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  predictedCategory?: 'B1' | 'B2'
  confidence?: number
  classProbabilities?: {
    B1?: number
    B2?: number
  }
  riskLevel?: 'LOW' | 'HIGH'
  modelName?: string
  doctorConfirmStatus?: string
  failureReason?: string | null
}
```

### 10.3 文件记录建议结构

如果前端后面要做文件记录选择器，建议至少使用下面这个结构：

```ts
type FileRecordOption = {
  id: number
  originalName: string
  bucketName?: string
  objectKey?: string
  fileType?: string
  contentType?: string
  fileSize?: number
  status?: string
}
```


## 11. 展示文案建议

### 11.1 类别说明

- `B1`：普通单纯头部外伤
- `B2`：复杂多发异常病例

### 11.2 风险说明

- `LOW`：低风险
- `HIGH`：高风险

### 11.3 状态说明

- `PENDING`：等待执行
- `RUNNING`：分析中
- `SUCCESS`：分析完成
- `FAILED`：分析失败

### 11.4 空状态说明

在任务尚未完成前，建议不要显示空白结果卡片，可以展示：

- “任务已提交，正在等待模型处理”

在还没选择文件记录前，可以展示：

- “请选择一条 CT 文件记录后再发起分析”


## 12. 推荐交互流程

建议前端按下面的真实链路实现：

1. 用户输入或选择 `ctImageFileId`
2. 前端提示该值对应 `file_record.id`
3. 用户点击 `开始分析`
4. 前端调用 `POST /api/ct-analysis/tasks`
5. 拿到 `taskId` 后进入轮询
6. 前端调用 `GET /api/ct-analysis/results/{taskId}`
7. 若任务成功，展示分类结果和概率
8. 若任务失败，展示失败原因


## 13. 当前联调建议

建议按以下顺序联调：

1. 先手工准备一条可用的 `file_record`
2. 确认这条记录的 `object_key` 指向本地路径，或者 `bucket_name + object_key` 能从 MinIO 读到
3. 前端手工输入 `ctImageFileId`
4. 调用创建任务接口
5. 观察是否返回 `taskId`
6. 轮询结果接口
7. 确认能看到 `SUCCESS` 或 `FAILED`
8. 跑通后再补文件列表和文件选择器


## 14. 当前限制

当前前端接入时需要注意：

- 只有一个分析类型，不需要做模型切换
- 当前没有通用文件上传接口
- 当前没有通用文件列表接口
- 当前没有热力图返回
- 当前没有异常切片定位能力
- 当前结果更适合先做成“任务提交 + 结果展示”
- 如果 MinIO 中保存的是压缩包，后端当前还没有自动解压逻辑


## 15. 推荐最小可用版本

如果你想先把 CT 页面快速搭起来，最小可用版本只需要：

- 一个 `ctImageFileId` 输入框
- 一个“开始分析”按钮
- 一个任务状态卡片
- 一个轮询逻辑
- 一个结果展示卡片
- 一个失败原因提示区

先跑通“已有文件记录 -> 创建任务 -> 查结果”这条主链路，再补文件管理页会更稳。


## 16. Axios 请求示例

```ts
import axios from 'axios'

export function createCtTask(ctImageFileId: number) {
  return axios.post('/api/ct-analysis/tasks', {
    ctImageFileId,
    analysisType: 'B1_B2_CLASSIFICATION'
  })
}

export function getCtResult(taskId: number) {
  return axios.get(`/api/ct-analysis/results/${taskId}`)
}
```


## 17. React 轮询示例

```tsx
import { useEffect, useRef, useState } from 'react'
import axios from 'axios'

export default function CtAnalysisPage() {
  const [ctImageFileId, setCtImageFileId] = useState('')
  const [taskId, setTaskId] = useState<number | null>(null)
  const [result, setResult] = useState<any>(null)
  const [submitting, setSubmitting] = useState(false)
  const timerRef = useRef<number | null>(null)

  const stopPolling = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current)
      timerRef.current = null
    }
  }

  const createTask = async () => {
    if (!ctImageFileId) return

    try {
      setSubmitting(true)
      stopPolling()
      setResult(null)

      const res = await axios.post('/api/ct-analysis/tasks', {
        ctImageFileId: Number(ctImageFileId),
        analysisType: 'B1_B2_CLASSIFICATION'
      })
      const newTaskId = res.data.data.taskId
      setTaskId(newTaskId)
    } finally {
      setSubmitting(false)
    }
  }

  useEffect(() => {
    if (!taskId) return

    const poll = async () => {
      const res = await axios.get(`/api/ct-analysis/results/${taskId}`)
      const data = res.data.data
      setResult(data)

      if (data.taskStatus === 'SUCCESS' || data.taskStatus === 'FAILED') {
        stopPolling()
      }
    }

    poll()
    timerRef.current = window.setInterval(poll, 2000)

    return stopPolling
  }, [taskId])

  return (
    <div>
      <h1>CT 分析</h1>
      <p>请输入 file_record.id 作为 ctImageFileId</p>
      <input
        value={ctImageFileId}
        onChange={(e) => setCtImageFileId(e.target.value)}
        placeholder="请输入 ctImageFileId"
      />
      <button disabled={!ctImageFileId || submitting} onClick={createTask}>
        {submitting ? '提交中' : '开始分析'}
      </button>

      {result && (
        <div>
          <p>任务状态: {result.taskStatus}</p>
          <p>预测类别: {result.predictedCategory}</p>
          <p>置信度: {result.confidence}</p>
          <p>风险等级: {result.riskLevel}</p>
          <p>失败原因: {result.failureReason}</p>
        </div>
      )}
    </div>
  )
}
```
