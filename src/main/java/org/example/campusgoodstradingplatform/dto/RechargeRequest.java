package org.example.campusgoodstradingplatform.dto;

import java.math.BigDecimal;

public record RechargeRequest(long userId, BigDecimal amount) {
}
