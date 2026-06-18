package com.neusoft.neu23.neuhospital.ct.service;

import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.ct.config.CtAnalysisProperties;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CtInputResolverService {

    private final MinioClient minioClient;
    private final CtAnalysisProperties ctAnalysisProperties;

    public CtInputResolverService(MinioClient minioClient, CtAnalysisProperties ctAnalysisProperties) {
        this.minioClient = minioClient;
        this.ctAnalysisProperties = ctAnalysisProperties;
    }

    public String resolveToLocalPath(Long taskId, FileRecordEntity fileRecordEntity) {
        if (!StringUtils.hasText(fileRecordEntity.getObjectKey())) {
            throw new BusinessException("CT 文件未记录 objectKey，无法发起分析");
        }

        Path localPathCandidate = Path.of(fileRecordEntity.getObjectKey());
        if (Files.exists(localPathCandidate)) {
            return localPathCandidate.toAbsolutePath().toString();
        }

        if (!StringUtils.hasText(fileRecordEntity.getBucketName())) {
            throw new BusinessException("CT 文件未记录 bucketName，无法从 MinIO 获取");
        }

        return downloadFromMinio(taskId, fileRecordEntity);
    }

    private String downloadFromMinio(Long taskId, FileRecordEntity fileRecordEntity) {
        try {
            Path taskCacheDir = createTaskCacheDir(taskId);
            String objectKey = normalizeObjectKey(fileRecordEntity.getObjectKey());
            String bucketName = fileRecordEntity.getBucketName();

            int downloaded = downloadAsPrefixIfExists(bucketName, objectKey, taskCacheDir);
            if (downloaded > 0) {
                return taskCacheDir.toAbsolutePath().toString();
            }

            Path downloadedFile = downloadSingleObject(bucketName, objectKey, taskCacheDir, fileRecordEntity.getOriginalName());
            return downloadedFile.toAbsolutePath().toString();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("从 MinIO 准备 CT 输入失败: " + ex.getMessage());
        }
    }

    private int downloadAsPrefixIfExists(String bucketName, String objectKey, Path taskCacheDir) throws Exception {
        String prefix = objectKey.endsWith("/") ? objectKey : objectKey + "/";
        int count = 0;
        for (var result : minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(true)
                        .build())) {
            Item item = result.get();
            if (item.isDir()) {
                continue;
            }
            String relativePath = item.objectName().substring(prefix.length());
            if (!StringUtils.hasText(relativePath)) {
                continue;
            }
            Path localTarget = taskCacheDir.resolve(relativePath);
            Files.createDirectories(localTarget.getParent());
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(item.objectName()).build())) {
                Files.copy(inputStream, localTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            count++;
        }
        return count;
    }

    private Path downloadSingleObject(String bucketName, String objectKey, Path taskCacheDir, String originalName) throws Exception {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectKey).build());
        } catch (ErrorResponseException ex) {
            throw new BusinessException("MinIO 中未找到 CT 对象: " + objectKey);
        }

        String fileName = StringUtils.hasText(originalName)
                ? originalName
                : Path.of(objectKey).getFileName().toString();
        Path localTarget = taskCacheDir.resolve(fileName);
        Files.createDirectories(localTarget.getParent());

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectKey).build())) {
            Files.copy(inputStream, localTarget, StandardCopyOption.REPLACE_EXISTING);
        }
        return localTarget;
    }

    private Path createTaskCacheDir(Long taskId) throws Exception {
        Path baseDir = Path.of(ctAnalysisProperties.getLocalCacheDir());
        Files.createDirectories(baseDir);
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path taskDir = baseDir.resolve("task-" + taskId + "-" + suffix);
        Files.createDirectories(taskDir);
        return taskDir;
    }

    private String normalizeObjectKey(String objectKey) {
        String normalized = objectKey.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
