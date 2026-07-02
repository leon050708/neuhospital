package com.neusoft.neu23.neuhospital.registration.controller;

import com.neusoft.neu23.neuhospital.auth.security.SecurityUtils;
import com.neusoft.neu23.neuhospital.registration.service.QueueService;
import com.neusoft.neu23.neuhospital.registration.vo.QueueItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDoctorQueue() {
        Long doctorId = SecurityUtils.getCurrentDoctorId();
        List<QueueItemVO> list = queueService.getDoctorQueue(doctorId);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "success");
        res.put("data", list);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/call")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> callPatient(@PathVariable Long id) {
        try {
            Long doctorId = SecurityUtils.getCurrentDoctorId();
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
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> skipPatient(@PathVariable Long id) {
        try {
            Long doctorId = SecurityUtils.getCurrentDoctorId();
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
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> finishPatient(@PathVariable Long id) {
        try {
            Long doctorId = SecurityUtils.getCurrentDoctorId();
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
