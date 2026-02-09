package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.ShippingWaveActionResultDto;
import com.wmsdipl.contracts.dto.ShippingWaveDto;
import com.wmsdipl.core.service.workflow.ShippingWaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/shipping/waves")
@Tag(name = "Shipping Waves", description = "Cross-dock wave shipping operations grouped by outboundRef")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
public class ShippingWaveController {

    private final ShippingWaveService shippingWaveService;

    public ShippingWaveController(ShippingWaveService shippingWaveService) {
        this.shippingWaveService = shippingWaveService;
    }

    @GetMapping
    @Operation(summary = "List shipping waves", description = "Returns cross-dock waves grouped by outboundRef")
    public List<ShippingWaveDto> listWaves() {
        return shippingWaveService.listWaves();
    }

    @PostMapping("/{outboundRef}/start")
    @Operation(summary = "Start shipping wave", description = "Starts shipping for all eligible receipts in outbound wave")
    public ResponseEntity<ShippingWaveActionResultDto> startWave(@PathVariable String outboundRef) {
        ShippingWaveActionResultDto result = shippingWaveService.startWave(outboundRef);
        if (result.targetedReceipts() == 0) {
            throw new ResponseStatusException(NOT_FOUND, "No cross-dock receipts found for outboundRef: " + outboundRef);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{outboundRef}/complete")
    @Operation(summary = "Complete shipping wave", description = "Completes shipping for all eligible receipts in outbound wave")
    public ResponseEntity<ShippingWaveActionResultDto> completeWave(@PathVariable String outboundRef) {
        ShippingWaveActionResultDto result = shippingWaveService.completeWave(outboundRef);
        if (result.targetedReceipts() == 0) {
            throw new ResponseStatusException(NOT_FOUND, "No cross-dock receipts found for outboundRef: " + outboundRef);
        }
        return ResponseEntity.ok(result);
    }
}
