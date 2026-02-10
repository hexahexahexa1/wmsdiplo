package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.contracts.dto.SkuUnitConfigDto;
import com.wmsdipl.contracts.dto.UpsertSkuUnitConfigsRequest;
import com.wmsdipl.core.domain.Discrepancy;
import com.wmsdipl.core.domain.ReceiptLine;
import com.wmsdipl.core.domain.ReceiptStatus;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.domain.SkuStatus;
import com.wmsdipl.core.domain.TaskStatus;
import com.wmsdipl.core.domain.SkuUnitConfig;
import com.wmsdipl.core.mapper.SkuMapper;
import com.wmsdipl.core.repository.PalletRepository;
import com.wmsdipl.core.repository.DiscrepancyRepository;
import com.wmsdipl.core.repository.ReceiptLineRepository;
import com.wmsdipl.core.repository.ScanRepository;
import com.wmsdipl.core.repository.SkuRepository;
import com.wmsdipl.core.repository.SkuUnitConfigRepository;
import com.wmsdipl.core.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service for managing SKU (Stock Keeping Unit) catalog.
 */
@Service
public class SkuService {

    private final SkuRepository skuRepository;
    private final SkuUnitConfigRepository skuUnitConfigRepository;
    private final ReceiptLineRepository receiptLineRepository;
    private final TaskRepository taskRepository;
    private final PalletRepository palletRepository;
    private final ScanRepository scanRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final SkuMapper skuMapper;
    private final AuditLogService auditLogService;

    private static final Set<ReceiptStatus> OPEN_RECEIPT_STATUSES = EnumSet.of(
        ReceiptStatus.DRAFT,
        ReceiptStatus.CONFIRMED,
        ReceiptStatus.IN_PROGRESS,
        ReceiptStatus.ACCEPTED,
        ReceiptStatus.READY_FOR_PLACEMENT,
        ReceiptStatus.PLACING,
        ReceiptStatus.READY_FOR_SHIPMENT,
        ReceiptStatus.SHIPPING_IN_PROGRESS
    );

    private static final Set<TaskStatus> ACTIVE_TASK_STATUSES = EnumSet.of(
        TaskStatus.NEW,
        TaskStatus.ASSIGNED,
        TaskStatus.IN_PROGRESS
    );

    public SkuService(
        SkuRepository skuRepository,
        SkuUnitConfigRepository skuUnitConfigRepository,
        ReceiptLineRepository receiptLineRepository,
        TaskRepository taskRepository,
        PalletRepository palletRepository,
        ScanRepository scanRepository,
        DiscrepancyRepository discrepancyRepository,
        SkuMapper skuMapper,
        AuditLogService auditLogService
    ) {
        this.skuRepository = skuRepository;
        this.skuUnitConfigRepository = skuUnitConfigRepository;
        this.receiptLineRepository = receiptLineRepository;
        this.taskRepository = taskRepository;
        this.palletRepository = palletRepository;
        this.scanRepository = scanRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.skuMapper = skuMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SkuDto> findAll() {
        return skuRepository.findAll().stream()
            .map(skuMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkuDto> findAllByStatus(SkuStatus status) {
        return skuRepository.findByStatus(status).stream()
            .map(skuMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SkuDto findById(Long id) {
        return skuRepository.findById(id)
            .map(skuMapper::toDto)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<SkuDto> findByCode(String code) {
        return skuRepository.findByCode(code)
            .map(skuMapper::toDto);
    }

    @Transactional
    public SkuDto create(CreateSkuRequest request) {
        if (skuRepository.findByCode(request.code()).isPresent()) {
            throw new ResponseStatusException(CONFLICT,
                "SKU with code '" + request.code() + "' already exists");
        }

        Sku sku = skuMapper.toEntity(request);
        sku.setStatus(SkuStatus.ACTIVE);
        try {
            Sku saved = skuRepository.save(sku);
            ensureDefaultBaseUnitConfig(saved, saved.getPalletCapacity());
            return skuMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT,
                "SKU with code '" + request.code() + "' already exists");
        }
    }

    @Transactional
    public SkuDto update(Long id, CreateSkuRequest request) {
        Sku sku = skuRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + id));

        Optional<Sku> existingWithCode = skuRepository.findByCode(request.code());
        if (existingWithCode.isPresent() && !existingWithCode.get().getId().equals(id)) {
            throw new ResponseStatusException(CONFLICT,
                "Another SKU with code '" + request.code() + "' already exists");
        }

        skuMapper.updateEntity(sku, request);
        Sku saved = skuRepository.save(sku);
        syncBaseUnitCodeWithSku(saved);
        return skuMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Sku sku = skuRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + id));

        List<String> blockers = collectDeleteBlockers(id);
        if (!blockers.isEmpty()) {
            throw new ResponseStatusException(
                CONFLICT,
                "SKU '" + sku.getCode() + "' cannot be deleted: " + String.join("; ", blockers)
            );
        }

        skuUnitConfigRepository.deleteAll(skuUnitConfigRepository.findBySkuIdOrderByIsBaseDescUnitCodeAsc(id));
        skuRepository.deleteById(id);
    }

    /**
     * Finds or creates an ACTIVE SKU by code.
     * Used during import to auto-create missing expected SKUs.
     */
    @Transactional
    public Sku findOrCreateActive(String code, String name, String uom) {
        Optional<Sku> existing = skuRepository.findByCode(code);
        if (existing.isPresent()) {
            Sku existingSku = existing.get();
            if (existingSku.getStatus() != SkuStatus.ACTIVE) {
                existingSku.setStatus(SkuStatus.ACTIVE);
                skuRepository.save(existingSku);
            }
            ensureDefaultBaseUnitConfig(existingSku, existingSku.getPalletCapacity());
            return existingSku;
        }

        Sku newSku = new Sku();
        newSku.setCode(code);
        newSku.setName(name != null && !name.isBlank() ? name : code);
        newSku.setUom(uom != null && !uom.isBlank() ? uom : "PCS");
        newSku.setStatus(SkuStatus.ACTIVE);

        Sku saved = skuRepository.save(newSku);
        ensureDefaultBaseUnitConfig(saved, saved.getPalletCapacity());
        return saved;
    }

    @Transactional
    public Sku findOrCreateDraftForBarcodeMismatch(String code, String name, String uom) {
        Optional<Sku> existing = skuRepository.findByCode(code);
        if (existing.isPresent()) {
            Sku existingSku = existing.get();
            ensureDefaultBaseUnitConfig(existingSku, existingSku.getPalletCapacity());
            return existingSku;
        }

        Sku draft = new Sku();
        draft.setCode(code);
        draft.setName(name != null && !name.isBlank() ? name : code);
        draft.setUom(uom != null && !uom.isBlank() ? uom : "PCS");
        draft.setStatus(SkuStatus.DRAFT);
        Sku saved = skuRepository.save(draft);
        ensureDefaultBaseUnitConfig(saved, saved.getPalletCapacity());
        return saved;
    }

    @Transactional
    public SkuDto approveDraft(Long id) {
        Sku sku = skuRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + id));
        if (sku.getStatus() != SkuStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only DRAFT SKU can be approved");
        }
        String oldStatus = sku.getStatus().name();
        sku.setStatus(SkuStatus.ACTIVE);
        Sku saved = skuRepository.save(sku);
        auditLogService.logStatusChange("SKU", saved.getId(), resolveCurrentUsername(), oldStatus, saved.getStatus().name());
        return skuMapper.toDto(saved);
    }

    @Transactional
    public SkuDto rejectDraft(Long id) {
        Sku sku = skuRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + id));
        if (sku.getStatus() != SkuStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Only DRAFT SKU can be rejected");
        }

        List<Long> relatedReceiptIds = collectRelatedReceiptIds(id);
        List<Long> blockedReceiptIds = relatedReceiptIds.stream()
            .filter(receiptId -> hasOperationalFactsForSkuInReceipt(receiptId, sku))
            .toList();
        if (!blockedReceiptIds.isEmpty()) {
            throw new ResponseStatusException(CONFLICT,
                "Cannot reject DRAFT SKU with operational facts in receipt(s): " + blockedReceiptIds
                    + ". Use remap instead.");
        }

        String oldStatus = sku.getStatus().name();
        sku.setStatus(SkuStatus.REJECTED);
        Sku saved = skuRepository.save(sku);
        auditLogService.logStatusChange("SKU", saved.getId(), resolveCurrentUsername(), oldStatus, saved.getStatus().name());

        List<ReceiptLine> lines = collectRejectCandidateLines(id);
        for (ReceiptLine line : lines) {
            line.setExcludedFromWorkflow(true);
            line.setExclusionReason("REJECTED_LINE");
        }
        if (!lines.isEmpty()) {
            receiptLineRepository.saveAll(lines);
        }
        return skuMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SkuUnitConfigDto> getUnitConfigs(Long skuId) {
        if (!skuRepository.existsById(skuId)) {
            throw new ResponseStatusException(NOT_FOUND, "SKU not found: " + skuId);
        }
        return skuUnitConfigRepository.findBySkuIdOrderByIsBaseDescUnitCodeAsc(skuId).stream()
            .map(this::toUnitConfigDto)
            .toList();
    }

    @Transactional
    public List<SkuUnitConfigDto> replaceUnitConfigs(Long skuId, UpsertSkuUnitConfigsRequest request) {
        Sku sku = skuRepository.findById(skuId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "SKU not found: " + skuId));

        List<UpsertSkuUnitConfigsRequest.Config> incoming = request.configs();
        validateIncomingConfigs(incoming);

        Map<String, UpsertSkuUnitConfigsRequest.Config> incomingByCode = new HashMap<>();
        for (UpsertSkuUnitConfigsRequest.Config cfg : incoming) {
            String code = normalizeUnitCode(cfg.unitCode());
            if (incomingByCode.putIfAbsent(code, cfg) != null) {
                throw new ResponseStatusException(BAD_REQUEST, "Duplicate unitCode in request: " + code);
            }
        }

        List<SkuUnitConfig> existing = skuUnitConfigRepository.findBySkuIdOrderByIsBaseDescUnitCodeAsc(skuId);
        Map<String, SkuUnitConfig> existingByCode = new HashMap<>();
        for (SkuUnitConfig cfg : existing) {
            existingByCode.put(normalizeUnitCode(cfg.getUnitCode()), cfg);
        }

        Set<String> blockedCodes = new HashSet<>();
        for (SkuUnitConfig cfg : existing) {
            String code = normalizeUnitCode(cfg.getUnitCode());
            UpsertSkuUnitConfigsRequest.Config target = incomingByCode.get(code);
            boolean removed = target == null;
            boolean becomesInactive = target != null && !Boolean.TRUE.equals(target.active());
            if ((removed || becomesInactive)
                && receiptLineRepository.existsBySkuIdAndUomIgnoreCaseAndReceipt_StatusIn(
                    skuId,
                    cfg.getUnitCode(),
                    OPEN_RECEIPT_STATUSES
                )) {
                blockedCodes.add(cfg.getUnitCode());
            }
        }
        if (!blockedCodes.isEmpty()) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Cannot remove/deactivate units used in open receipts: " + String.join(", ", blockedCodes)
            );
        }

        List<SkuUnitConfig> toSave = incoming.stream()
            .map(cfg -> {
                String normalizedCode = normalizeUnitCode(cfg.unitCode());
                SkuUnitConfig entity = existingByCode.getOrDefault(normalizedCode, new SkuUnitConfig());
                entity.setSkuId(skuId);
                entity.setUnitCode(normalizedCode);
                entity.setFactorToBase(cfg.factorToBase().setScale(6, RoundingMode.HALF_UP));
                entity.setUnitsPerPallet(cfg.unitsPerPallet().setScale(3, RoundingMode.HALF_UP));
                entity.setIsBase(cfg.isBase());
                entity.setActive(cfg.active());
                return entity;
            })
            .toList();

        Set<String> incomingCodes = incomingByCode.keySet();
        List<SkuUnitConfig> toDelete = existing.stream()
            .filter(cfg -> !incomingCodes.contains(normalizeUnitCode(cfg.getUnitCode())))
            .toList();
        if (!toDelete.isEmpty()) {
            skuUnitConfigRepository.deleteAll(toDelete);
        }

        skuUnitConfigRepository.saveAll(toSave);

        SkuUnitConfig base = toSave.stream()
            .filter(cfg -> Boolean.TRUE.equals(cfg.getIsBase()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Base unit is required"));
        sku.setUom(base.getUnitCode());
        skuRepository.save(sku);

        return skuUnitConfigRepository.findBySkuIdOrderByIsBaseDescUnitCodeAsc(skuId).stream()
            .map(this::toUnitConfigDto)
            .toList();
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public SkuUnitConfig getActiveUnitConfigOrThrow(Long skuId, String unitCode) {
        String normalizedCode = normalizeUnitCode(unitCode);
        SkuUnitConfig config = skuUnitConfigRepository.findBySkuIdAndUnitCodeIgnoreCase(skuId, normalizedCode)
            .orElseThrow(() -> new ResponseStatusException(
                BAD_REQUEST,
                "Unit '" + normalizedCode + "' is not configured for SKU id " + skuId
            ));
        if (!Boolean.TRUE.equals(config.getActive())) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Unit '" + normalizedCode + "' is inactive for SKU id " + skuId
            );
        }
        if (config.getUnitsPerPallet() == null || config.getUnitsPerPallet().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Unit '" + normalizedCode + "' has invalid unitsPerPallet for SKU id " + skuId
            );
        }
        return config;
    }

    private void validateIncomingConfigs(List<UpsertSkuUnitConfigsRequest.Config> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one unit config is required");
        }
        long baseCount = configs.stream().filter(cfg -> Boolean.TRUE.equals(cfg.isBase())).count();
        if (baseCount != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Exactly one base unit must be defined");
        }
        for (UpsertSkuUnitConfigsRequest.Config cfg : configs) {
            BigDecimal factor = cfg.factorToBase();
            BigDecimal unitsPerPallet = cfg.unitsPerPallet();
            if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "factorToBase must be > 0");
            }
            if (unitsPerPallet == null || unitsPerPallet.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "unitsPerPallet must be > 0");
            }
            if (factor.scale() > 6) {
                throw new ResponseStatusException(BAD_REQUEST, "factorToBase supports up to 6 decimal places");
            }
            if (unitsPerPallet.scale() > 3) {
                throw new ResponseStatusException(BAD_REQUEST, "unitsPerPallet supports up to 3 decimal places");
            }
            if (Boolean.TRUE.equals(cfg.isBase()) && factor.compareTo(BigDecimal.ONE) != 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Base unit factorToBase must equal 1");
            }
            if (Boolean.TRUE.equals(cfg.isBase()) && !Boolean.TRUE.equals(cfg.active())) {
                throw new ResponseStatusException(BAD_REQUEST, "Base unit must be active");
            }
        }
    }

    private void syncBaseUnitCodeWithSku(Sku sku) {
        SkuUnitConfig base = skuUnitConfigRepository.findBySkuIdAndIsBaseTrue(sku.getId()).orElse(null);
        if (base == null) {
            ensureDefaultBaseUnitConfig(sku, sku.getPalletCapacity());
            return;
        }

        String normalizedSkuUom = normalizeUnitCode(sku.getUom());
        if (normalizedSkuUom.equals(normalizeUnitCode(base.getUnitCode()))) {
            return;
        }

        Optional<SkuUnitConfig> conflicting = skuUnitConfigRepository.findBySkuIdAndUnitCodeIgnoreCase(
            sku.getId(),
            normalizedSkuUom
        );
        if (conflicting.isPresent() && !conflicting.get().getId().equals(base.getId())) {
            throw new ResponseStatusException(
                BAD_REQUEST,
                "Cannot set base UOM to '" + normalizedSkuUom + "' because this unit already exists"
            );
        }

        base.setUnitCode(normalizedSkuUom);
        base.setFactorToBase(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP));
        if (base.getUnitsPerPallet() == null || base.getUnitsPerPallet().compareTo(BigDecimal.ZERO) <= 0) {
            base.setUnitsPerPallet(defaultUnitsPerPallet(sku.getPalletCapacity()));
        }
        base.setActive(true);
        skuUnitConfigRepository.save(base);
    }

    private void ensureDefaultBaseUnitConfig(Sku sku, BigDecimal legacyPalletCapacity) {
        if (skuUnitConfigRepository.findBySkuIdAndIsBaseTrue(sku.getId()).isPresent()) {
            return;
        }
        SkuUnitConfig config = new SkuUnitConfig();
        config.setSkuId(sku.getId());
        config.setUnitCode(normalizeUnitCode(sku.getUom()));
        config.setFactorToBase(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP));
        config.setUnitsPerPallet(defaultUnitsPerPallet(legacyPalletCapacity));
        config.setIsBase(true);
        config.setActive(true);
        skuUnitConfigRepository.save(config);
    }

    private BigDecimal defaultUnitsPerPallet(BigDecimal legacyPalletCapacity) {
        if (legacyPalletCapacity != null && legacyPalletCapacity.compareTo(BigDecimal.ZERO) > 0) {
            return legacyPalletCapacity.setScale(3, RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
    }

    private SkuUnitConfigDto toUnitConfigDto(SkuUnitConfig config) {
        return new SkuUnitConfigDto(
            config.getId(),
            config.getSkuId(),
            config.getUnitCode(),
            config.getFactorToBase(),
            config.getUnitsPerPallet(),
            config.getIsBase(),
            config.getActive()
        );
    }

    private String normalizeUnitCode(String unitCode) {
        if (unitCode == null || unitCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "unitCode is required");
        }
        return unitCode.trim().toUpperCase(Locale.ROOT);
    }

    private List<Long> collectRelatedReceiptIds(Long skuId) {
        Set<Long> receiptIds = new HashSet<>();
        receiptLineRepository.findBySkuId(skuId).stream()
            .map(ReceiptLine::getReceipt)
            .filter(java.util.Objects::nonNull)
            .map(com.wmsdipl.core.domain.Receipt::getId)
            .filter(java.util.Objects::nonNull)
            .forEach(receiptIds::add);

        discrepancyRepository.findByDraftSkuId(skuId).stream()
            .map(Discrepancy::getReceipt)
            .filter(java.util.Objects::nonNull)
            .map(com.wmsdipl.core.domain.Receipt::getId)
            .filter(java.util.Objects::nonNull)
            .forEach(receiptIds::add);

        return receiptIds.stream().sorted().toList();
    }

    private List<ReceiptLine> collectRejectCandidateLines(Long skuId) {
        Map<Long, ReceiptLine> result = new HashMap<>();
        receiptLineRepository.findBySkuId(skuId).forEach(line -> {
            if (line != null && line.getId() != null) {
                result.put(line.getId(), line);
            }
        });

        discrepancyRepository.findByDraftSkuId(skuId).stream()
            .map(Discrepancy::getLine)
            .filter(java.util.Objects::nonNull)
            .forEach(line -> {
                if (line.getId() != null) {
                    result.putIfAbsent(line.getId(), line);
                }
            });

        return result.values().stream().toList();
    }

    private boolean hasOperationalFactsForSkuInReceipt(Long receiptId, Sku sku) {
        if (receiptId == null || sku == null || sku.getId() == null) {
            return false;
        }
        if (taskRepository.existsByReceiptIdAndLine_SkuId(receiptId, sku.getId())) {
            return true;
        }
        if (palletRepository.existsByReceipt_IdAndSkuId(receiptId, sku.getId())) {
            return true;
        }
        return scanRepository.existsByTask_Receipt_IdAndBarcodeIgnoreCase(receiptId, sku.getCode());
    }

    private List<String> collectDeleteBlockers(Long skuId) {
        List<String> blockers = new ArrayList<>();

        if (palletRepository.existsBySkuIdAndQuantityGreaterThan(skuId, BigDecimal.ZERO)) {
            blockers.add("current stock quantity is greater than zero");
        }
        if (receiptLineRepository.existsActiveBySkuIdAndReceiptStatusIn(skuId, OPEN_RECEIPT_STATUSES)) {
            blockers.add("SKU is used in open receipt lines");
        }
        if (taskRepository.existsByLine_SkuIdAndStatusIn(skuId, ACTIVE_TASK_STATUSES)) {
            blockers.add("SKU has open tasks");
        }
        boolean hasLineDiscrepancies = discrepancyRepository.existsByLine_SkuIdAndResolvedFalseAndReceipt_StatusIn(
            skuId,
            OPEN_RECEIPT_STATUSES
        );
        boolean hasDraftDiscrepancies = discrepancyRepository.existsByDraftSkuIdAndResolvedFalseAndReceipt_StatusIn(
            skuId,
            OPEN_RECEIPT_STATUSES
        );
        if (hasLineDiscrepancies || hasDraftDiscrepancies) {
            blockers.add("SKU has unresolved discrepancies in open receipts");
        }

        return blockers;
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()
            || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "system";
        }
        return authentication.getName();
    }
}
