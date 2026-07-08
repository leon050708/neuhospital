package com.neusoft.neu23.neuhospital.pharmacy.controller;

import com.neusoft.neu23.neuhospital.auth.security.GatewayHeaderAuthenticationFilter;
import com.neusoft.neu23.neuhospital.auth.security.SecurityConfig;
import com.neusoft.neu23.neuhospital.pharmacy.service.DispenseService;
import com.neusoft.neu23.neuhospital.pharmacy.service.DrugInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DrugController.class, DispenseController.class})
@Import({SecurityConfig.class, GatewayHeaderAuthenticationFilter.class})
class PharmacyWritePermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DrugInfoService drugInfoService;

    @MockBean
    private DispenseService dispenseService;

    @Test
    void pharmacistCanCreateDrug() throws Exception {
        when(drugInfoService.createDrug(any())).thenReturn(101L);

        mockMvc.perform(post("/api/drugs")
                        .headers(userHeaders("PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "drugName": "阿莫西林胶囊",
                                  "salePrice": 15.50,
                                  "stockQuantity": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(101));
    }

    @Test
    void adminCannotCreateDrug() throws Exception {
        mockMvc.perform(post("/api/drugs")
                        .headers(userHeaders("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "drugName": "阿莫西林胶囊",
                                  "salePrice": 15.50,
                                  "stockQuantity": 100
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(drugInfoService, never()).createDrug(any());
    }

    @Test
    void pharmacistCanAdjustStock() throws Exception {
        mockMvc.perform(post("/api/drugs/101/stock-adjust")
                        .headers(userHeaders("PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adjustQuantity": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(drugInfoService).adjustStock(eq(101L), eq(20));
    }

    @Test
    void adminCannotAdjustStock() throws Exception {
        mockMvc.perform(post("/api/drugs/101/stock-adjust")
                        .headers(userHeaders("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adjustQuantity": 20
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(drugInfoService, never()).adjustStock(any(), any());
    }

    @Test
    void pharmacistCanDispense() throws Exception {
        when(dispenseService.dispense(any())).thenReturn(201L);

        mockMvc.perform(post("/api/pharmacy/dispense")
                        .headers(userHeaders("PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prescriptionId": 301,
                                  "pharmacyUserId": 9511
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(201));
    }

    @Test
    void adminCannotDispense() throws Exception {
        mockMvc.perform(post("/api/pharmacy/dispense")
                        .headers(userHeaders("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prescriptionId": 301,
                                  "pharmacyUserId": 9511
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(dispenseService, never()).dispense(any());
    }

    private HttpHeaders userHeaders(String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "9511");
        headers.set("X-Username", "pharmacist_demo");
        headers.set("X-User-Roles", role);
        headers.set("X-User-Type", "MANAGEMENT");
        return headers;
    }
}
