package com.neusoft.neu23.neuhospital.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.file.entity.FileRecordEntity;
import com.neusoft.neu23.neuhospital.file.service.FileService;
import com.neusoft.neu23.neuhospital.file.vo.FilePreviewVO;
import com.neusoft.neu23.neuhospital.file.vo.FileRecordVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileRecordVO> uploadFile(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "bizType", required = false) String bizType,
                                           @RequestParam(value = "bizId", required = false) Long bizId,
                                           @RequestParam(value = "bucketName", required = false) String bucketName,
                                           @RequestParam(value = "objectKeyPrefix", required = false) String objectKeyPrefix,
                                           @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return Result.success(fileService.uploadFile(file, bizType, bizId, bucketName, objectKeyPrefix, uploaderId));
    }

    @PostMapping(value = "/upload-directory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileRecordVO> uploadDirectory(@RequestParam("files") MultipartFile[] files,
                                                @RequestParam("relativePaths") String[] relativePaths,
                                                @RequestParam(value = "caseName", required = false) String caseName,
                                                @RequestParam(value = "bizType", required = false) String bizType,
                                                @RequestParam(value = "bizId", required = false) Long bizId,
                                                @RequestParam(value = "bucketName", required = false) String bucketName,
                                                @RequestParam(value = "objectKeyPrefix", required = false) String objectKeyPrefix,
                                                @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return Result.success(fileService.uploadDirectory(
                files, relativePaths, caseName, bizType, bizId, bucketName, objectKeyPrefix, uploaderId
        ));
    }

    @GetMapping
    public Result<PageResult<FileRecordVO>> getFilesPage(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                         @RequestParam(value = "bizType", required = false) String bizType,
                                                         @RequestParam(value = "bizId", required = false) Long bizId,
                                                         @RequestParam(value = "keyword", required = false) String keyword) {
        Page<FileRecordVO> page = fileService.getFilesPage(pageNo, pageSize, bizType, bizId, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @GetMapping("/{fileId}")
    public Result<FileRecordVO> getFile(@PathVariable("fileId") Long fileId) {
        return Result.success(fileService.getFileById(fileId));
    }

    @GetMapping("/{fileId}/preview-url")
    public Result<FilePreviewVO> getPreviewUrl(@PathVariable("fileId") Long fileId,
                                               @RequestParam(value = "expiresInSeconds", required = false) Integer expiresInSeconds) {
        return Result.success(fileService.createPreviewUrl(fileId, expiresInSeconds));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("fileId") Long fileId) throws Exception {
        FileService.DownloadFile downloadFile = fileService.downloadFile(fileId);
        FileRecordEntity entity = downloadFile.fileRecord();
        try (InputStream inputStream = downloadFile.inputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentHeader(entity.getOriginalName()))
                    .contentType(resolveMediaType(entity.getContentType()))
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<byte[]> previewFile(@PathVariable("fileId") Long fileId) throws Exception {
        FileService.DownloadFile downloadFile = fileService.downloadFile(fileId);
        FileRecordEntity entity = downloadFile.fileRecord();
        try (InputStream inputStream = downloadFile.inputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(resolveMediaType(entity.getContentType()))
                    .contentLength(bytes.length)
                    .body(bytes);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String buildAttachmentHeader(String originalName) {
        String safeName = originalName == null ? "download.bin" : originalName;
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
