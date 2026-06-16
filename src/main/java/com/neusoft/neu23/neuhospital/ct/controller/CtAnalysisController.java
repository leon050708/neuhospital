package com.neusoft.neu23.neuhospital.ct.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.ct.dto.CtAnalysisTaskCreateReq;
import com.neusoft.neu23.neuhospital.ct.service.CtAnalysisService;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisResultVO;
import com.neusoft.neu23.neuhospital.ct.vo.CtAnalysisTaskVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ct-analysis")
public class CtAnalysisController {

    private final CtAnalysisService ctAnalysisService;

    public CtAnalysisController(CtAnalysisService ctAnalysisService) {
        this.ctAnalysisService = ctAnalysisService;
    }

    @PostMapping("/tasks")
    public Result<CtAnalysisTaskVO> createTask(@RequestBody CtAnalysisTaskCreateReq req) {
        return Result.success(ctAnalysisService.createTask(req));
    }

    @GetMapping("/results/{taskId}")
    public Result<CtAnalysisResultVO> getResult(@PathVariable("taskId") Long taskId) {
        return Result.success(ctAnalysisService.getResult(taskId));
    }
}
