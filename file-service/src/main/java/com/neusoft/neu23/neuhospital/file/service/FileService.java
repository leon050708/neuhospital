package com.neusoft.neu23.neuhospital.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.vo.FilePreviewVO;
import com.neusoft.neu23.neuhospital.file.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {

    FileRecordVO uploadFile(MultipartFile file,
                            String bizType,
                            Long bizId,
                            String bucketName,
                            String objectKeyPrefix,
                            Long uploaderId);

    FileRecordVO uploadDirectory(MultipartFile[] files,
                                 String[] relativePaths,
                                 String caseName,
                                 String bizType,
                                 Long bizId,
                                 String bucketName,
                                 String objectKeyPrefix,
                                 Long uploaderId);

    Page<FileRecordVO> getFilesPage(Integer pageNo, Integer pageSize, String bizType, Long bizId, String keyword);

    FileRecordVO getFileById(Long fileId);

    FilePreviewVO createPreviewUrl(Long fileId, Integer expiresInSeconds);

    DownloadFile downloadFile(Long fileId);

    record DownloadFile(FileRecordEntity fileRecord, InputStream inputStream) {
    }
}
