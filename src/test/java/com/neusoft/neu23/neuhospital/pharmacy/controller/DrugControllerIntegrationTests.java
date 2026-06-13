package com.neusoft.neu23.neuhospital.pharmacy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugInfoCreateReq;
import com.neusoft.neu23.neuhospital.pharmacy.dto.DrugStockAdjustReq;
import com.neusoft.neu23.neuhospital.pharmacy.entity.DrugInfoEntity;
import com.neusoft.neu23.neuhospital.pharmacy.service.DrugInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 保证每个测试后数据回滚
public class DrugControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DrugInfoService drugInfoService;

    @BeforeEach
    void setUp() {
        // 清理一下数据（虽然有 Transactional 但如果需要也可以执行删除）
    }

    @Test
    void testCreateAndGetDrug() throws Exception {
        // 1. 新增药品
        DrugInfoCreateReq req = new DrugInfoCreateReq();
        req.setDrugName("阿莫西林胶囊");
        req.setGenericName("阿莫西林");
        req.setSpecification("0.25g*50粒");
        req.setUnit("盒");
        req.setCategory("抗生素");
        req.setSalePrice(new BigDecimal("15.50"));
        req.setStockQuantity(100);

        MvcResult result = mockMvc.perform(post("/api/drugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Long newDrugId = objectMapper.readTree(content).get("data").asLong();
        assertNotNull(newDrugId);

        // 2. 查询详情
        mockMvc.perform(get("/api/drugs/" + newDrugId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.drugName").value("阿莫西林胶囊"))
                .andExpect(jsonPath("$.data.stockQuantity").value(100));
    }

    @Test
    void testAdjustStock() throws Exception {
        // 1. 先准备一条药品数据
        DrugInfoCreateReq req = new DrugInfoCreateReq();
        req.setDrugName("布洛芬缓释胶囊");
        req.setSalePrice(new BigDecimal("22.00"));
        req.setStockQuantity(50);
        Long newDrugId = drugInfoService.createDrug(req);

        // 2. 调整库存 (入库 20)
        DrugStockAdjustReq adjustReq = new DrugStockAdjustReq();
        adjustReq.setAdjustQuantity(20);

        mockMvc.perform(post("/api/drugs/" + newDrugId + "/stock-adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adjustReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3. 验证新库存为 70
        DrugInfoEntity entity = drugInfoService.getById(newDrugId);
        assertEquals(70, entity.getStockQuantity());
        
        // 4. 调整库存 (盘亏减少 80)，预期会报错因为只有 70
        DrugStockAdjustReq badAdjustReq = new DrugStockAdjustReq();
        badAdjustReq.setAdjustQuantity(-80);
        
        mockMvc.perform(post("/api/drugs/" + newDrugId + "/stock-adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badAdjustReq)))
                .andExpect(status().isOk()) // Global exception handler 捕获后返回 500
                .andExpect(jsonPath("$.code").value(400)); // BusinessException 400
    }
}
