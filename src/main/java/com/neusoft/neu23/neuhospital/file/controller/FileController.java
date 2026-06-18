package com.neusoft.neu23.neuhospital.file.controller;

import com.neusoft.neu23.neuhospital.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "GENERAL") String bizType,
            @RequestParam(value = "bizId", required = false) Long bizId,
            @RequestHeader(value = "X-User-Id", required = false) Long uploaderId) {
        
        if (uploaderId == null) {
            uploaderId = 99999L; // Mock System user or unknown
        }

        try {
            String url = fileService.uploadFile(file, bizType, bizId, uploaderId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "文件上传成功");
            res.put("data", url);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 500);
            err.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }
}
