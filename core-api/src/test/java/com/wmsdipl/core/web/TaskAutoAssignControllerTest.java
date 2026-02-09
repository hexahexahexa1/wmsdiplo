package com.wmsdipl.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wmsdipl.contracts.dto.AutoAssignPreviewItemDto;
import com.wmsdipl.contracts.dto.AutoAssignResultDto;
import com.wmsdipl.core.service.TaskAutoAssignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskAutoAssignController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskAutoAssignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskAutoAssignService taskAutoAssignService;

    @Test
    void shouldDryRunAutoAssign() throws Exception {
        AutoAssignResultDto result = new AutoAssignResultDto(
            1,
            1,
            0,
            List.of(new AutoAssignPreviewItemDto(10L, null, "operator1", 0, "ASSIGN"))
        );
        when(taskAutoAssignService.dryRun(any())).thenReturn(result);

        mockMvc.perform(post("/api/tasks/auto-assign/dry-run")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskIds\":[10],\"reassignAssigned\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedCount").value(1))
            .andExpect(jsonPath("$.items[0].suggestedAssignee").value("operator1"));
    }

    @Test
    void shouldApplyAutoAssign() throws Exception {
        AutoAssignResultDto result = new AutoAssignResultDto(
            1,
            1,
            0,
            List.of(new AutoAssignPreviewItemDto(10L, null, "operator1", 0, "ASSIGN"))
        );
        when(taskAutoAssignService.apply(any())).thenReturn(result);

        mockMvc.perform(post("/api/tasks/auto-assign/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskIds\":[10],\"reassignAssigned\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCandidates").value(1));
    }
}
