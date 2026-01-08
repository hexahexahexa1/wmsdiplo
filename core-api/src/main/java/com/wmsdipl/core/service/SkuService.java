package com.wmsdipl.core.service;

import com.wmsdipl.contracts.dto.CreateSkuRequest;
import com.wmsdipl.contracts.dto.SkuDto;
import com.wmsdipl.core.domain.Sku;
import com.wmsdipl.core.mapper.SkuMapper;
import com.wmsdipl.core.repository.SkuRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Service for managing SKU (Stock Keeping Unit) catalog.
 */
@Service
public class SkuService {

    private final SkuRepository skuRepository;
    private final SkuMapper skuMapper;

    public SkuService(SkuRepository skuRepository, SkuMapper skuMapper) {
        this.skuRepository = skuRepository;
        this.skuMapper = skuMapper;
    }

    @Transactional(readOnly = true)
    public List<SkuDto> findAll() {
        return skuRepository.findAll().stream()
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
        // Check for duplicate code
        if (skuRepository.findByCode(request.code()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, 
                "SKU with code '" + request.code() + "' already exists");
        }

        Sku sku = skuMapper.toEntity(request);
        try {
            Sku saved = skuRepository.save(sku);
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

        // Check if new code conflicts with another SKU
        Optional<Sku> existingWithCode = skuRepository.findByCode(request.code());
        if (existingWithCode.isPresent() && !existingWithCode.get().getId().equals(id)) {
            throw new ResponseStatusException(CONFLICT, 
                "Another SKU with code '" + request.code() + "' already exists");
        }

        skuMapper.updateEntity(sku, request);
        Sku saved = skuRepository.save(sku);
        return skuMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!skuRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "SKU not found: " + id);
        }
        skuRepository.deleteById(id);
    }

    /**
     * Finds or creates a SKU by code.
     * Used during import to auto-create missing SKUs.
     * 
     * @param code SKU code
     * @param name SKU name (used if creating new)
     * @param uom Unit of measure (used if creating new, defaults to "ШТ" if null)
     * @return Existing or newly created SKU
     */
    @Transactional
    public Sku findOrCreate(String code, String name, String uom) {
        Optional<Sku> existing = skuRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new SKU
        Sku newSku = new Sku();
        newSku.setCode(code);
        newSku.setName(name != null && !name.isBlank() ? name : code);
        newSku.setUom(uom != null && !uom.isBlank() ? uom : "ШТ");
        
        return skuRepository.save(newSku);
    }
}
