package com.neusoft.neu23.neuhospital.image.controller;

import com.neusoft.neu23.neuhospital.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/ct/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCtImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "registrationId", required = false) Long registrationId,
            @RequestHeader(value = "X-User-Id", required = false) Long uploaderId) {

        if (uploaderId == null) uploaderId = 30001L; // Mock doctor
        if (registrationId == null) registrationId = 1L; // Mock registration

        try {
            // bizType 指定为 CT_ORIGINAL, bizId 绑定到就诊/挂号单的 ID 上
            String url = fileService.uploadFile(file, "CT_ORIGINAL", registrationId, uploaderId);
            
            // TODO: 后续在这里可以发送 Kafka 消息，通知 AI 模型进行脑出血检测
            
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "CT 影像上传成功，等待 AI 分析");
            res.put("data", url);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 500);
            err.put("message", "CT 上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}
