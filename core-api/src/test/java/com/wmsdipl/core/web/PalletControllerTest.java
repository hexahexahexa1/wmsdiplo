package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.PalletDto;
import com.wmsdipl.core.domain.Pallet;
import com.wmsdipl.core.domain.PalletStatus;
import com.wmsdipl.core.mapper.PalletMapper;
import com.wmsdipl.core.service.CsvExportService;
import com.wmsdipl.core.service.PalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for PalletController using MockMvc.
 * Tests pallet management endpoints and CSV export functionality.
 */
@WebMvcTest(PalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class PalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PalletService palletService;

    @MockBean
    private PalletMapper palletMapper;

    @MockBean
    private CsvExportService csvExportService;

    @Test
    void shouldExportAllPallets_WhenCalled() throws Exception {
        // Given
        Pallet pallet1 = createPallet(1L, "PLT-001");
        Pallet pallet2 = createPallet(2L, "PLT-002");
        List<Pallet> pallets = List.of(pallet1, pallet2);
        byte[] csvContent = "ID,Code\n1,PLT-001\n2,PLT-002".getBytes();

        when(palletService.getAll()).thenReturn(pallets);
        when(csvExportService.exportPallets(pallets)).thenReturn(csvContent);

        // When & Then
        mockMvc.perform(get("/api/pallets/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"pallets.csv\""))
                .andExpect(content().bytes(csvContent));

        verify(palletService).getAll();
        verify(csvExportService).exportPallets(pallets);
    }

    @Test
    void shouldReturnEmptyCSV_WhenNoPalletsExist() throws Exception {
        // Given
        byte[] csvContent = "ID,Code\n".getBytes();

        when(palletService.getAll()).thenReturn(List.of());
        when(csvExportService.exportPallets(any())).thenReturn(csvContent);

        // When & Then
        mockMvc.perform(get("/api/pallets/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        verify(palletService).getAll();
    }

    // Helper methods

    private Pallet createPallet(Long id, String code) {
        Pallet pallet = new Pallet();
        try {
            java.lang.reflect.Field idField = Pallet.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pallet, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        pallet.setCode(code);
        pallet.setCodeType("INTERNAL");
        pallet.setStatus(PalletStatus.EMPTY);
        pallet.setSkuId(1L);
        pallet.setQuantity(BigDecimal.TEN);
        pallet.setUom("ШТ");

        return pallet;
    }
}
