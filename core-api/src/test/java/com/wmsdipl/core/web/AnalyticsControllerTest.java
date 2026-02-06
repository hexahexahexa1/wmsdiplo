package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.ReceivingAnalyticsDto;
import com.wmsdipl.contracts.dto.ReceivingHealthDto;
import com.wmsdipl.core.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shouldReturnAnalytics_WhenDateRangeValid() throws Exception {
        ReceivingAnalyticsDto dto = new ReceivingAnalyticsDto(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 7),
            2.5,
            1.5,
            Map.of("ACCEPTED", 3),
            Map.of("DAMAGE", 1),
            33.3,
            Map.of("DAMAGED", 2),
            10.0
        );
        when(analyticsService.calculateAnalytics(any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/receiving?fromDate=2026-02-01&toDate=2026-02-07"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fromDate").value("2026-02-01"))
            .andExpect(jsonPath("$.toDate").value("2026-02-07"))
            .andExpect(jsonPath("$.avgReceivingTimeHours").value(2.5))
            .andExpect(jsonPath("$.avgPlacingTimeHours").value(1.5))
            .andExpect(jsonPath("$.receiptsByStatus.ACCEPTED").value(3));
    }

    @Test
    void shouldReturnBadRequest_WhenDateRangeInvalid() throws Exception {
        mockMvc.perform(get("/api/analytics/receiving?fromDate=2026-02-07&toDate=2026-02-01"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCsv_WhenExportRequested() throws Exception {
        byte[] csv = "metric,value\nfromDate,2026-02-01\n".getBytes();
        when(analyticsService.exportAnalyticsCsv(any(), any())).thenReturn(csv);

        mockMvc.perform(get("/api/analytics/export-csv?fromDate=2026-02-01&toDate=2026-02-07"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(content().bytes(csv));
    }

    @Test
    void shouldReturnReceivingHealth_WhenDateRangeValid() throws Exception {
        ReceivingHealthDto dto = new ReceivingHealthDto(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 7),
            4,
            2L,
            1L,
            5L,
            3L,
            4L
        );
        when(analyticsService.calculateReceivingHealth(any(), any(), any(Integer.class))).thenReturn(dto);

        mockMvc.perform(get("/api/analytics/receiving-health?fromDate=2026-02-01&toDate=2026-02-07&thresholdHours=4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stuckReceivingReceipts").value(2))
            .andExpect(jsonPath("$.staleTasks").value(5));
    }
}
