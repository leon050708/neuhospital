package com.neusoft.neu23.neuhospital.pharmacy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neusoft.neu23.neuhospital.common.response.PageResult;
import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoUpdateReq;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugStockAdjustReq;
import com.neusoft.neu23.neuhospital.pharmacy.service.DrugInfoService;
import com.neusoft.neu23.neuhospital.pharmacy.vo.DrugInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drugs")
public class DrugController {

    @Autowired
    private DrugInfoService drugInfoService;

    @PostMapping
    public Result<Long> createDrug(@RequestBody DrugInfoCreateReq req) {
        Long id = drugInfoService.createDrug(req);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Void> updateDrug(@PathVariable Long id, @RequestBody DrugInfoUpdateReq req) {
        drugInfoService.updateDrug(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/stock-adjust")
    public Result<Void> adjustStock(@PathVariable Long id, @RequestBody DrugStockAdjustReq req) {
        drugInfoService.adjustStock(id, req.getAdjustQuantity());
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<DrugInfoVO> getDrugById(@PathVariable Long id) {
        DrugInfoVO vo = drugInfoService.getDrugById(id);
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<DrugInfoVO>> getDrugsPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category) {
        Page<DrugInfoVO> page = drugInfoService.getDrugsPage(pageNo, pageSize, keyword, category);
        return Result.success(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
}
