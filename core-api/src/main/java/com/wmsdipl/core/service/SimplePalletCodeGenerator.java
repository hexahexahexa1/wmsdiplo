package com.wmsdipl.core.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class SimplePalletCodeGenerator implements PalletCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public List<String> generateInternalCodes(String prefix, int count) {
        String safePrefix = prefix == null ? "PLT" : prefix;
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        while (result.size() < count) {
            String candidate = safePrefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            if (unique.add(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    @Override
    public List<String> generateSSCC(String companyPrefix, int count) {
        String prefix = companyPrefix == null ? "0000000" : companyPrefix;
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String base = prefix + String.format("%09d", RANDOM.nextInt(1_000_000_000));
            String withCheckDigit = base + calculateCheckDigit(base);
            result.add(withCheckDigit);
        }
        return result;
    }

    @Override
    public boolean validateCode(String code) {
        return code != null && code.length() >= 8 && code.length() <= 64;
    }

    private int calculateCheckDigit(String digits) {
        // GS1 mod-10 algorithm
        int sum = 0;
        boolean evenPosition = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = Character.digit(digits.charAt(i), 10);
            sum += evenPosition ? d * 3 : d;
            evenPosition = !evenPosition;
        }
        int mod = sum % 10;
        return (10 - mod) % 10;
    }
}
