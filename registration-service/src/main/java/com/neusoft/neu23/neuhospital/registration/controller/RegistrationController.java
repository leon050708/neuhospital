package com.neusoft.neu23.neuhospital.registration.controller;

import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> quickRegister(@Validated @RequestBody RegistrationCreateReq req) {
        try {
            req.setPatientId(SecurityUtils.getCurrentPatientId());
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
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyRegistrations(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtils.getCurrentPatientId();
        Page<RegistrationEntity> page = registrationService.getMyRegistrations(userId, pageNo, pageSize);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", page);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','REGISTRATION_CLERK')")
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
    @PreAuthorize("@accessEvaluator.canAccessRegistration(#id, authentication)")
    public ResponseEntity<?> getRegistrationById(@PathVariable Long id) {
        RegistrationEntity reg = registrationService.getRegistrationById(id);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", reg);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> cancelRegistration(@PathVariable Long id) {
        try {
            Long userId = SecurityUtils.getCurrentPatientId();
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
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> checkIn(@PathVariable Long id) {
        try {
            Long userId = SecurityUtils.getCurrentPatientId();
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
