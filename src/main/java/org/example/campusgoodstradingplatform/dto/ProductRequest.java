package org.example.campusgoodstradingplatform.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        Long id,
        String name,
        String category,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        String size,
        List<String> photos,
        String usageGuide,
        boolean negotiable,
        int stock,
        String condition
) {
}
