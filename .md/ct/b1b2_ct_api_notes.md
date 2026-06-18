# B1/B2 CT 分类接入说明

## 1. Python 推理脚本

脚本：

- `/Users/leon/PycharmProjects/neuedu/ct_demo1/infer_b1_b2.py`

用途：

- 输入一个病例目录或 DICOM 序列目录
- 自动读取最佳平扫序列
- 调用训练好的 `best_b1_b2_model.pth`
- 输出病例级 `B1/B2` 分类结果 JSON

示例命令：

```bash
/opt/anaconda3/envs/neuedu/bin/python /Users/leon/PycharmProjects/neuedu/ct_demo1/infer_b1_b2.py \
  --input "/Users/leon/PycharmProjects/neuedu/ct_demo1/data/CQ500CT427 CQ500CT427"
```

返回示例：

```json
{
  "analysisType": "B1_B2_CLASSIFICATION",
  "inputPath": "/path/to/case",
  "seriesDir": "/path/to/case/CT PLAIN",
  "predictedCategory": "B2",
  "confidence": 0.912345,
  "classProbabilities": {
    "B1": 0.087655,
    "B2": 0.912345
  },
  "riskLevel": "HIGH",
  "modelName": "cq500_b1_b2_classifier_v1",
  "device": "mps",
  "numSlices": 16,
  "imageSize": 224
}
```

## 2. Java 调用方式

推荐在 `ai` 或 `image` 模块中增加一个服务类，通过 `ProcessBuilder` 调 Python。

示例：

```java
@Service
public class CtAiInferenceService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public B1B2InferenceResult infer(String caseDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "/opt/anaconda3/envs/neuedu/bin/python",
                "/Users/leon/PycharmProjects/neuedu/ct_demo1/infer_b1_b2.py",
                "--input",
                caseDir
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (InputStream is = process.getInputStream()) {
            output = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Python 推理失败: " + output);
        }

        return objectMapper.readValue(output, B1B2InferenceResult.class);
    }
}
```

结果对象：

```java
@Data
public class B1B2InferenceResult {
    private String analysisType;
    private String inputPath;
    private String seriesDir;
    private String predictedCategory;
    private Double confidence;
    private Map<String, Double> classProbabilities;
    private String riskLevel;
    private String modelName;
    private String device;
    private Integer numSlices;
    private Integer imageSize;
}
```

## 3. 修改后的接口说明

### 11.3 CT 分析任务接口

```text
POST /api/ct-analysis/tasks
```

请求体建议：

```json
{
  "ctImageFileId": 90001,
  "analysisType": "B1_B2_CLASSIFICATION"
}
```

说明：

- `ctImageFileId` 对应已上传登记的 CT 文件
- `analysisType` 当前接入 `B1_B2_CLASSIFICATION`
- 后端创建任务后，异步调用 Python 推理脚本

### 11.4 CT 分析结果接口

```text
GET /api/ct-analysis/results/{taskId}
```

结果响应建议：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "analysisType": "B1_B2_CLASSIFICATION",
    "predictedCategory": "B2",
    "confidence": 0.91,
    "classProbabilities": {
      "B1": 0.09,
      "B2": 0.91
    },
    "riskLevel": "HIGH",
    "modelName": "cq500_b1_b2_classifier_v1",
    "doctorConfirmStatus": "UNCONFIRMED"
  },
  "timestamp": "2026-06-16T10:00:00Z"
}
```

字段建议解释：

- `predictedCategory`
  - `B1`：普通单纯头部外伤，无复杂并发症
  - `B2`：复杂多发异常病例，合并多种出血、骨折或颅内高压征象
- `confidence`
  - 当前预测类别的概率
- `classProbabilities`
  - 两类完整概率，便于前端展示与医生复核
- `riskLevel`
  - 可以先做简单映射：`B1 -> LOW`，`B2 -> HIGH`

## 4. 接口适配建议

你当前模型是病例级分类，不适合直接返回下面这些字段：

- `hasHemorrhage`
- `abnormalSlices`
- `heatmapFileId`

因为当前 `B1/B2` 模型没有切片定位和热力图输出能力。

如果未来再训练：

- 出血检测模型
- 异常切片定位模型
- 可视化热力图模型

再把这些字段加回去更合适。
