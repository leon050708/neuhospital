package com.neusoft.neu23.neuhospital.registration.controller;

import com.neusoft.neu23.neuhospital.registration.dto.RegistrationCreateReq;
import com.neusoft.neu23.neuhospital.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
}
