package com.neusoft.neu23.neuhospital.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.exception.BusinessException;
import com.neusoft.neu23.neuhospital.ct.config.MinioProperties;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.mapper.FileRecordMapper;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import com.neusoft.neu23.neuhospital.file.vo.FilePreviewVO;
import com.neusoft.neu23.neuhospital.file.vo.FileRecordVO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PREVIEW_EXPIRE_SECONDS = 3600;

    private final FileRecordMapper fileRecordMapper;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public FileServiceImpl(FileRecordMapper fileRecordMapper,
                           MinioClient minioClient,
                           MinioProperties minioProperties) {
        this.fileRecordMapper = fileRecordMapper;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public FileRecordVO uploadFile(MultipartFile file,
                                   String bizType,
                                   Long bizId,
                                   String bucketName,
                                   String objectKeyPrefix,
                                   Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String resolvedBucket = resolveBucketName(bucketName);
        ensureBucketExists(resolvedBucket);

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "unnamed-file";
        String objectKey = buildObjectKey(objectKeyPrefix, originalName);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("文件上传到 MinIO 失败: " + ex.getMessage());
        }

        FileRecordEntity entity = new FileRecordEntity();
        entity.setBizType(normalizeText(bizType));
        entity.setBizId(bizId);
        entity.setBucketName(resolvedBucket);
        entity.setObjectKey(objectKey);
        entity.setOriginalName(originalName);
        entity.setFileType(extractFileType(originalName));
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploaderId(uploaderId);
        entity.setUploadedAt(LocalDateTime.now());
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);
        fileRecordMapper.insert(entity);

        return toVO(entity);
    }

    @Override
    public FileRecordVO uploadDirectory(MultipartFile[] files,
                                        String[] relativePaths,
                                        String caseName,
                                        String bizType,
                                        Long bizId,
                                        String bucketName,
                                        String objectKeyPrefix,
                                        Long uploaderId) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传目录不能为空");
        }
        if (relativePaths == null || relativePaths.length != files.length) {
            throw new BusinessException("relativePaths 数量必须和 files 一致");
        }

        String resolvedBucket = resolveBucketName(bucketName);
        ensureBucketExists(resolvedBucket);

        String resolvedCaseName = resolveCaseName(caseName, relativePaths);
        String directoryObjectKey = buildDirectoryObjectKey(objectKeyPrefix, resolvedCaseName);
        long totalSize = 0L;
        List<String> uploadedObjectKeys = new ArrayList<>();

        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String relativePath = sanitizeRelativePath(relativePaths[i], file.getOriginalFilename());
                String objectKey = directoryObjectKey + "/" + relativePath;
                try (InputStream inputStream = file.getInputStream()) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(resolvedBucket)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                uploadedObjectKeys.add(objectKey);
                totalSize += file.getSize();
            }
        } catch (Exception ex) {
            throw new BusinessException("目录上传到 MinIO 失败: " + ex.getMessage());
        }

        if (uploadedObjectKeys.isEmpty()) {
            throw new BusinessException("上传目录中没有有效文件");
        }

        FileRecordEntity entity = new FileRecordEntity();
        entity.setBizType(normalizeText(bizType));
        entity.setBizId(bizId);
        entity.setBucketName(resolvedBucket);
        entity.setObjectKey(directoryObjectKey);
        entity.setOriginalName(resolvedCaseName);
        entity.setFileType("directory");
        entity.setContentType("application/x-directory");
        entity.setFileSize(totalSize);
        entity.setUploaderId(uploaderId);
        entity.setUploadedAt(LocalDateTime.now());
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);
        fileRecordMapper.insert(entity);

        return toVO(entity);
    }

    @Override
    public Page<FileRecordVO> getFilesPage(Integer pageNo, Integer pageSize, String bizType, Long bizId, String keyword) {
        Page<FileRecordEntity> page = new Page<>(
                pageNo != null ? pageNo : DEFAULT_PAGE_NO,
                pageSize != null ? pageSize : DEFAULT_PAGE_SIZE
        );
        QueryWrapper<FileRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (StringUtils.hasText(bizType)) {
            wrapper.eq("biz_type", bizType.trim());
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("original_name", keyword.trim())
                    .or()
                    .like("object_key", keyword.trim()));
        }
        wrapper.orderByDesc("uploaded_at").orderByDesc("id");
        fileRecordMapper.selectPage(page, wrapper);

        List<FileRecordVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        Page<FileRecordVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public FileRecordVO getFileById(Long fileId) {
        return toVO(requireFile(fileId));
    }

    @Override
    public FilePreviewVO createPreviewUrl(Long fileId, Integer expiresInSeconds) {
        FileRecordEntity entity = requireFile(fileId);
        int expireSeconds = expiresInSeconds != null && expiresInSeconds > 0
                ? expiresInSeconds
                : DEFAULT_PREVIEW_EXPIRE_SECONDS;
        try {
            String previewUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(entity.getBucketName())
                    .object(entity.getObjectKey())
                    .expiry(expireSeconds)
                    .build());
            FilePreviewVO vo = new FilePreviewVO();
            vo.setFileId(entity.getId());
            vo.setPreviewUrl(previewUrl);
            vo.setExpiresInSeconds(expireSeconds);
            return vo;
        } catch (Exception ex) {
            throw new BusinessException("生成预览链接失败: " + ex.getMessage());
        }
    }

    @Override
    public DownloadFile downloadFile(Long fileId) {
        FileRecordEntity entity = requireFile(fileId);
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(entity.getBucketName())
                    .object(entity.getObjectKey())
                    .build());
            return new DownloadFile(entity, inputStream);
        } catch (Exception ex) {
            throw new BusinessException("下载文件失败: " + ex.getMessage());
        }
    }

    private FileRecordEntity requireFile(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("fileId 不能为空");
        }
        FileRecordEntity entity = fileRecordMapper.selectById(fileId);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException("文件不存在");
        }
        return entity;
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception ex) {
            throw new BusinessException("初始化 MinIO Bucket 失败: " + ex.getMessage());
        }
    }

    private String resolveBucketName(String bucketName) {
        if (StringUtils.hasText(bucketName)) {
            return bucketName.trim();
        }
        String defaultBucket = minioProperties.getBucket() != null
                ? minioProperties.getBucket().getCtOriginal()
                : null;
        if (StringUtils.hasText(defaultBucket)) {
            return defaultBucket.trim();
        }
        throw new BusinessException("未指定 bucketName，且未配置默认 CT Bucket");
    }

    private String buildObjectKey(String objectKeyPrefix, String originalName) {
        String cleanName = originalName.replace("\\", "_").replace("/", "_").trim();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = normalizePrefix(objectKeyPrefix);
        return prefix + uuid + "-" + cleanName;
    }

    private String buildDirectoryObjectKey(String objectKeyPrefix, String caseName) {
        String cleanCaseName = caseName.replace("\\", "_").replace("/", "_").trim();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = normalizePrefix(objectKeyPrefix);
        return prefix + uuid + "-" + cleanCaseName;
    }

    private String normalizePrefix(String objectKeyPrefix) {
        if (!StringUtils.hasText(objectKeyPrefix)) {
            return "uploads/";
        }
        String prefix = objectKeyPrefix.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    private String extractFileType(String originalName) {
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return null;
        }
        return originalName.substring(dotIndex + 1).toLowerCase();
    }

    private String resolveCaseName(String caseName, String[] relativePaths) {
        if (StringUtils.hasText(caseName)) {
            return caseName.trim();
        }
        for (String relativePath : relativePaths) {
            if (!StringUtils.hasText(relativePath)) {
                continue;
            }
            String sanitized = relativePath.replace("\\", "/").trim();
            int slashIndex = sanitized.indexOf('/');
            if (slashIndex > 0) {
                return sanitized.substring(0, slashIndex);
            }
        }
        return "ct-case-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String sanitizeRelativePath(String relativePath, String fallbackName) {
        String resolved = StringUtils.hasText(relativePath) ? relativePath.trim() : fallbackName;
        if (!StringUtils.hasText(resolved)) {
            throw new BusinessException("目录文件缺少相对路径和文件名");
        }
        String normalized = resolved.replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        String[] segments = normalized.split("/");
        List<String> safeSegments = new ArrayList<>();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || ".".equals(segment) || "..".equals(segment)) {
                continue;
            }
            safeSegments.add(segment.trim());
        }
        if (safeSegments.isEmpty()) {
            throw new BusinessException("目录文件相对路径不合法: " + resolved);
        }
        return String.join("/", safeSegments);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private FileRecordVO toVO(FileRecordEntity entity) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(entity.getId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setBucketName(entity.getBucketName());
        vo.setObjectKey(entity.getObjectKey());
        vo.setOriginalName(entity.getOriginalName());
        vo.setFileType(entity.getFileType());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setUploaderId(entity.getUploaderId());
        vo.setUploadedAt(entity.getUploadedAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
