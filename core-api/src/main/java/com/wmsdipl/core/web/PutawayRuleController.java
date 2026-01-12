package com.wmsdipl.core.web;

import com.wmsdipl.contracts.dto.PutawayRuleDto;
import com.wmsdipl.core.domain.PutawayRule;
import com.wmsdipl.core.domain.Zone;
import com.wmsdipl.core.mapper.PutawayRuleMapper;
import com.wmsdipl.core.repository.PutawayRuleRepository;
import com.wmsdipl.core.repository.ZoneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/putaway-rules")
@Tag(name = "Putaway Rules", description = "Configuration of putaway strategies and rules for location selection")
public class PutawayRuleController {

    private final PutawayRuleRepository repository;
    private final ZoneRepository zoneRepository;
    private final PutawayRuleMapper mapper;

    public PutawayRuleController(PutawayRuleRepository repository, 
                                 ZoneRepository zoneRepository,
                                 PutawayRuleMapper mapper) {
        this.repository = repository;
        this.zoneRepository = zoneRepository;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List all putaway rules", description = "Retrieves all configured putaway rules for location selection strategies")
    @Transactional(readOnly = true)
    public List<PutawayRuleDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get putaway rule by ID", description = "Retrieves a single putaway rule by its unique identifier")
    @Transactional(readOnly = true)
    public ResponseEntity<PutawayRuleDto> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create putaway rule", description = "Creates a new putaway rule with the specified strategy type and parameters")
    public ResponseEntity<PutawayRuleDto> create(@Valid @RequestBody PutawayRuleDto dto) {
        PutawayRule rule = mapper.toEntity(dto);
        applyZone(rule, dto.zoneId());
        rule.setCreatedAt(LocalDateTime.now());
        PutawayRule saved = repository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update putaway rule", description = "Updates an existing putaway rule's configuration")
    public ResponseEntity<PutawayRuleDto> update(@PathVariable Long id, @Valid @RequestBody PutawayRuleDto dto) {
        return repository.findById(id).map(rule -> {
            updateFromDto(rule, dto);
            PutawayRule saved = repository.save(rule);
            return ResponseEntity.ok(mapper.toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete putaway rule", description = "Removes a putaway rule from the system")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void updateFromDto(PutawayRule rule, PutawayRuleDto dto) {
        if (dto.priority() != null) rule.setPriority(dto.priority());
        if (dto.name() != null) rule.setName(dto.name());
        if (dto.strategyType() != null) rule.setStrategyType(dto.strategyType());
        applyZone(rule, dto.zoneId());
        if (dto.skuCategory() != null) rule.setSkuCategory(dto.skuCategory());
        if (dto.velocityClass() != null) rule.setVelocityClass(dto.velocityClass());
        if (dto.params() != null) rule.setParams(dto.params());
        if (dto.active() != null) rule.setActive(dto.active());
    }

    private void applyZone(PutawayRule rule, Long zoneId) {
        if (zoneId != null) {
            Zone zone = zoneRepository.findById(zoneId).orElse(null);
            rule.setZone(zone);
        } else {
            rule.setZone(null);
        }
    }
}

