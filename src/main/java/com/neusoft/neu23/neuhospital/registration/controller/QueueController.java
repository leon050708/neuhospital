package com.neusoft.neu23.neuhospital.registration.controller;

import com.neusoft.neu23.neuhospital.registration.service.QueueService;
import com.neusoft.neu23.neuhospital.registration.vo.QueueItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @GetMapping("/doctor/me")
    public ResponseEntity<?> getDoctorQueue(
            @RequestHeader(value = "X-User-Id", required = false) Long doctorId) {
        if (doctorId == null) {
            doctorId = 30001L; // Mock doctorId if not provided
        }
        List<QueueItemVO> list = queueService.getDoctorQueue(doctorId);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/call")
    public ResponseEntity<?> callPatient(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long doctorId) {
        if (doctorId == null) doctorId = 30001L;
        try {
            queueService.callPatient(id, doctorId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "叫号成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<?> skipPatient(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long doctorId) {
        if (doctorId == null) doctorId = 30001L;
        try {
            queueService.skipPatient(id, doctorId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "过号成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<?> finishPatient(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long doctorId) {
        if (doctorId == null) doctorId = 30001L;
        try {
            queueService.finishPatient(id, doctorId);
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("message", "就诊完成");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
