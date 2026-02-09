package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.ShippingWaveActionResultDto;
import com.wmsdipl.contracts.dto.ShippingWaveDto;
import com.wmsdipl.core.service.workflow.ShippingWaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShippingWaveController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShippingWaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShippingWaveService shippingWaveService;

    @Test
    void shouldListWaves() throws Exception {
        ShippingWaveDto wave = new ShippingWaveDto("OUT-1", 2, 1, 1, 0, 3, 0, "IN_PROGRESS");
        when(shippingWaveService.listWaves()).thenReturn(List.of(wave));

        mockMvc.perform(get("/api/shipping/waves"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].outboundRef").value("OUT-1"))
            .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));
    }

    @Test
    void shouldStartWave() throws Exception {
        ShippingWaveActionResultDto result = new ShippingWaveActionResultDto("OUT-2", 1, 1, 2, List.of(), List.of());
        when(shippingWaveService.startWave("OUT-2")).thenReturn(result);

        mockMvc.perform(post("/api/shipping/waves/OUT-2/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasksCreated").value(2));
    }
}
