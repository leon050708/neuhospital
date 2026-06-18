package com.neusoft.neu23.neuhospital.file.service.impl;

import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.mapper.FileRecordMapper;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket.report:medical-report}")
    private String defaultBucket;

    @Override
    public String uploadFile(MultipartFile file, String bizType, Long bizId, Long uploaderId) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 为了防止文件名冲突，使用 UUID 作为对象键名
            String objectKey = UUID.randomUUID().toString().replace("-", "") + extension;

            // 根据业务类型决定存入哪个 Bucket
            String bucketName = determineBucket(bizType);

            // 确保 Bucket 存在 (如果不存在则自动创建，方便开发)
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // 执行上传
            InputStream is = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            is.close();

            // 将文件记录落库，实现与具体的患者或业务绑定
            FileRecordEntity record = new FileRecordEntity();
            record.setBizType(bizType);
            record.setBizId(bizId);
            record.setBucketName(bucketName);
            record.setObjectKey(objectKey);
            record.setOriginalName(originalFilename);
            record.setFileType(extension);
            record.setContentType(file.getContentType());
            record.setFileSize(file.getSize());
            record.setUploaderId(uploaderId);
            record.setUploadedAt(LocalDateTime.now());
            record.setStatus("SUCCESS");
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setDeleted(false);

            fileRecordMapper.insert(record);

            // 返回文件的完整访问地址 (这里简单拼接，正式环境如果是前端直连一般只需要相对路径，或者走 CDN)
            return endpoint + "/" + bucketName + "/" + objectKey;
        } catch (Exception e) {
            throw new RuntimeException("文件上传到 MinIO 失败: " + e.getMessage(), e);
        }
    }

    private String determineBucket(String bizType) {
        if ("KNOWLEDGE_DOC".equals(bizType)) {
            return "knowledge-docs";
        } else if ("CT_ORIGINAL".equals(bizType)) {
            return "medical-ct-original";
        }
        // 默认存放到 report bucket
        return defaultBucket;
    }
}
