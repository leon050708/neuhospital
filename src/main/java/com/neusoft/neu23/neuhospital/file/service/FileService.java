package com.neusoft.neu23.neuhospital.file.service;

import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 上传文件到 MinIO 并记录到数据库
     *
     * @param file       文件对象
     * @param bizType    业务类型 (例如: PATIENT_HISTORY, KNOWLEDGE_DOC, CT_ORIGINAL)
     * @param bizId      业务ID (例如: 患者ID、知识库ID)
     * @param uploaderId 上传者用户ID
     * @return 返回文件的可访问 URL 或存储路径
     */
    String uploadFile(MultipartFile file, String bizType, Long bizId, Long uploaderId);
}
