package com.wmsdipl.core.service;

import com.wmsdipl.core.repository.DiscrepancyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscrepancyJournalConfigService {

    private static final String RETENTION_KEY = "discrepancy_retention_days";
    private static final int DEFAULT_RETENTION_DAYS = 180;

    private final JdbcTemplate jdbcTemplate;
    private final DiscrepancyRepository discrepancyRepository;

    public DiscrepancyJournalConfigService(
        JdbcTemplate jdbcTemplate,
        DiscrepancyRepository discrepancyRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.discrepancyRepository = discrepancyRepository;
    }

    @Transactional(readOnly = true)
    public int getRetentionDays() {
        List<String> values = jdbcTemplate.query(
            "SELECT config_value FROM import_config WHERE config_key = ?",
            (rs, rowNum) -> rs.getString("config_value"),
            RETENTION_KEY
        );
        if (values.isEmpty()) {
            return DEFAULT_RETENTION_DAYS;
        }
        String value = values.get(0);
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : DEFAULT_RETENTION_DAYS;
        } catch (NumberFormatException ex) {
            return DEFAULT_RETENTION_DAYS;
        }
    }

    @Transactional
    public int updateRetentionDays(int retentionDays) {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays must be greater than 0");
        }
        jdbcTemplate.update(
            "INSERT INTO import_config (config_key, config_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = CURRENT_TIMESTAMP",
            RETENTION_KEY,
            String.valueOf(retentionDays)
        );
        return retentionDays;
    }

    @Transactional
    public long cleanupExpiredEntries() {
        int retentionDays = getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return discrepancyRepository.deleteByCreatedAtBefore(cutoff);
    }
}
