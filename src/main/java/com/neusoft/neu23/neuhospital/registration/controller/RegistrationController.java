package com.neusoft.neu23.neuhospital.registration.controller;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.registration.entity.RegistrationEntity;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/quick")
    public ResponseEntity<?> quickRegister(@Validated @RequestBody RegistrationCreateReq req) {
        try {
            String msgId = registrationService.quickRegister(req);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "抢号受理成功，正在排队出票中");
            res.put("msgId", msgId); // 供前端轮询状态使用
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyRegistrations(
            @RequestHeader(value = "X-User-Id", required = false) Long userId, // 简化：从网关/拦截器获取用户ID
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 假设这里通过拦截器注入了 userId，为了演示先支持传参或者默认
        if (userId == null) {
            userId = 20001L; // Mock userId if not provided
        }
        Page<RegistrationEntity> page = registrationService.getMyRegistrations(userId, pageNo, pageSize);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", page);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<?> getAllRegistrations(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<RegistrationEntity> page = registrationService.getAllRegistrations(pageNo, pageSize);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", page);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRegistrationById(@PathVariable Long id) {
        RegistrationEntity reg = registrationService.getRegistrationById(id);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", reg);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRegistration(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            registrationService.cancelRegistration(id, userId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "退号成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            registrationService.checkIn(id, userId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "报到签到成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
