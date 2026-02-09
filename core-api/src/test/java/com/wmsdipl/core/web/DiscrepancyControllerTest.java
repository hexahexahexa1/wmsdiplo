package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.DiscrepancyDto;
import com.wmsdipl.core.config.SecurityConfig;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.mapper.DiscrepancyMapper;
import com.wmsdipl.core.repository.UserRepository;
import com.wmsdipl.core.service.DiscrepancyJournalConfigService;
import com.wmsdipl.core.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiscrepancyController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class DiscrepancyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private DiscrepancyMapper discrepancyMapper;

    @MockBean
    private DiscrepancyJournalConfigService discrepancyJournalConfigService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = {"SUPERVISOR"})
    void shouldReturnDiscrepancyJournal_WithFilters() throws Exception {
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setTaskId(10L);

        DiscrepancyDto dto = new DiscrepancyDto(
            1L,
            100L,
            "RCP-001",
            200L,
            1,
            10L,
            300L,
            400L,
            "operator1",
            "UNDER_QTY",
            new BigDecimal("10.00"),
            new BigDecimal("8.00"),
            "comment",
            false,
            null,
            null,
            LocalDateTime.now()
        );

        when(taskService.findDiscrepancyJournal(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(discrepancy));
        when(taskService.findTaskAssigneesByDiscrepancies(any())).thenReturn(Map.of(10L, "operator1"));
        when(taskService.resolveDiscrepancyOperator(eq(discrepancy), any())).thenReturn("operator1");
        when(discrepancyMapper.toDto(eq(discrepancy), eq("operator1"))).thenReturn(dto);
        when(discrepancyJournalConfigService.getRetentionDays()).thenReturn(180);

        mockMvc.perform(get("/api/discrepancies")
                .param("type", "UNDER_QTY")
                .param("operator", "operator1")
                .param("fromDate", "2026-02-01")
                .param("toDate", "2026-02-07"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].operator").value("operator1"));

        verify(taskService).findDiscrepancyJournal(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = {"SUPERVISOR"})
    void shouldUpdateDiscrepancyComment() throws Exception {
        Discrepancy discrepancy = new Discrepancy();
        discrepancy.setTaskId(10L);

        DiscrepancyDto dto = new DiscrepancyDto(
            1L,
            100L,
            "RCP-001",
            200L,
            1,
            10L,
            300L,
            400L,
            "operator1",
            "UNDER_QTY",
            new BigDecimal("10.00"),
            new BigDecimal("8.00"),
            "updated",
            false,
            null,
            null,
            LocalDateTime.now()
        );

        when(taskService.updateDiscrepancyComment(1L, "updated")).thenReturn(discrepancy);
        when(discrepancyMapper.toDto(discrepancy)).thenReturn(dto);

        mockMvc.perform(patch("/api/discrepancies/1/comment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comment").value("updated"));

        verify(taskService).updateDiscrepancyComment(1L, "updated");
    }

    @Test
    @WithMockUser(roles = {"SUPERVISOR"})
    void shouldReturnRetentionConfig() throws Exception {
        when(discrepancyJournalConfigService.getRetentionDays()).thenReturn(180);

        mockMvc.perform(get("/api/discrepancies/retention"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retentionDays").value(180));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void shouldUpdateRetentionConfig() throws Exception {
        when(discrepancyJournalConfigService.updateRetentionDays(90)).thenReturn(90);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/discrepancies/retention")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retentionDays\":90}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retentionDays").value(90));

        verify(discrepancyJournalConfigService).updateRetentionDays(90);
    }

    @Test
    @WithMockUser(roles = {"OPERATOR"})
    void shouldReturnForbidden_WhenOperatorRequestsJournal() throws Exception {
        mockMvc.perform(get("/api/discrepancies"))
            .andExpect(status().isForbidden());
    }
}
