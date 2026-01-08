package com.wmsdipl.core.service;

import java.util.List;

public interface PalletCodeGenerator {
    List<String> generateInternalCodes(String prefix, int count);
    List<String> generateSSCC(String companyPrefix, int count);
    boolean validateCode(String code);
}
