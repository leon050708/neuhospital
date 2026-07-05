package com.neusoft.neu23.neuhospital.pharmacy.controller;

import com.neusoft.neu23.neuhospital.common.response.Result;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DispenseReq;
import com.neusoft.neu23.neuhospital.pharmacy.service.DispenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pharmacy/dispense")
public class DispenseController {

    @Autowired
    private DispenseService dispenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public Result<Long> dispense(@RequestBody DispenseReq req) {
        return Result.success(dispenseService.dispense(req));
    }
}
