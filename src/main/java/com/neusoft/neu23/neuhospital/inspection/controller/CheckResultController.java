package com.neusoft.neu23.neuhospital.inspection.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.inspection.dto.CheckResultCreateReq;
import com.neusoft.neu23.neuhospital.inspection.service.CheckResultService;
import com.neusoft.neu23.neuhospital.inspection.vo.CheckResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/check-results")
public class CheckResultController {

    @Autowired
    private CheckResultService checkResultService;

    @PostMapping
    public Result<Long> recordResult(@RequestBody CheckResultCreateReq req) {
        Long id = checkResultService.recordResult(req);
        return Result.success(id);
    }

    @GetMapping("/{id}")
    public Result<CheckResultVO> getResultDetail(@PathVariable Long id) {
        return Result.success(checkResultService.getResultDetail(id));
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirmResult(@PathVariable Long id) {
        checkResultService.confirmResult(id);
        return Result.success(null);
    }
}
